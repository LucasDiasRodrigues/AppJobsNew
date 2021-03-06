package com.teamappjobs.appjobs.activity;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.squareup.picasso.Picasso;
import com.teamappjobs.appjobs.R;
import com.teamappjobs.appjobs.adapter.PagerAdapterBuscar;
import com.teamappjobs.appjobs.adapter.PagerAdapterHome;
import com.teamappjobs.appjobs.asyncTask.BuscaDadosPerfilTask;
import com.teamappjobs.appjobs.asyncTask.ListaResultadoBuscaTask;
import com.teamappjobs.appjobs.asyncTask.LogOutTask;
import com.teamappjobs.appjobs.fragment.ConfiguracoesFragment;
import com.teamappjobs.appjobs.fragment.ConversasFragment;
import com.teamappjobs.appjobs.fragment.MinhasVitrinesFragment;
import com.teamappjobs.appjobs.fragment.VitrinesSigoFragment;
import com.teamappjobs.appjobs.modelo.Usuario;
import com.teamappjobs.appjobs.modelo.Vitrine;
import com.teamappjobs.appjobs.util.BuscarEventBus;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    //Fabs
    private FloatingActionButton fabCadastrarVitrine;
    private FloatingActionButton fabExemplo;
    private SearchView searchView;

    //Tabs
    private Toolbar toolbar;
    private ViewPager mViewPager;
    private int numTabs = 2;
    private TabLayout mTabLayout;

    //Naavigation
    private NavigationView navigationView;
    private View frameLayout;
    private CircleImageView imagemPerfil;
    private TextView txtNome;
    private TextView txtEmail;

    //Login
    private boolean logado;
    private Usuario usuario;

    //PagerAdapterMain
    private PagerAdapterHome pagerAdapterMain;

    //PagerAdapterBusca
    private PagerAdapterBuscar pagerAdapterBusca;
    private boolean isBuscaActive = false;

    //Firebase
    private FirebaseAnalytics mFirebaseAnalytics;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Firebase
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //Navigation
        frameLayout = findViewById(R.id.frameLayout);
        configuraNavigationView();

        //Fabs
        fabExemplo = (FloatingActionButton) findViewById(R.id.fab);
        fabExemplo.hide();
        fabCadastrarVitrine = (FloatingActionButton) findViewById(R.id.fabCadastroVitrine);
        fabCadastrarVitrine.hide();
        fabCadastrarVitrine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CadastroVitrineActivity.class);
                intent.putExtra("editar", false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(intent,
                            ActivityOptions.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                } else {
                    startActivity(intent);
                }
            }
        });

        //Configurando as tabs e fragments
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        ligaTabsMain(true);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (isBuscaActive) {
            ligaTabsBusca(false);
            ligaTabsMain(true);
            navigationView.setCheckedItem(R.id.home);
            toolbar.setTitle(R.string.app_name);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        //Altera opcao se nao logado
        if (logado) {
            menu.findItem(R.id.entrar).setVisible(false);
        } else {
            menu.findItem(R.id.sair).setVisible(false);
        }

        // Barra de perquisa
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ligaTabsMain(false);
                ligaTabsBusca(true);
                Pesquisa(query);
                fabCadastrarVitrine.hide();
                fabExemplo.hide();
                fabCadastrarVitrine.hide();
                toolbar.setTitle(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String searchQuery) {
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.entrar) {
            Intent intent = new Intent(this, LoginActivity.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                startActivity(intent,
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            } else {
                startActivity(intent);
            }

            return true;
        } else if (id == R.id.sair) {

            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage(R.string.desejasair)
                    .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onLogout();
                        }
                    }).setNegativeButton(R.string.nao, null).show();

            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.home) {
            ligaTabsBusca(false);
            ligaTabsMain(true);
            fabCadastrarVitrine.hide();
            fabExemplo.hide();
            toolbar.setTitle(R.string.app_name);


        } else if (id == R.id.minhasVitrines) {
            ligaTabsBusca(false);
            ligaTabsMain(false);
            MinhasVitrinesFragment fragmentMinhasVitrines = new MinhasVitrinesFragment();
            android.support.v4.app.FragmentTransaction transactionMinhasVitrines = getSupportFragmentManager().beginTransaction();
            transactionMinhasVitrines.replace(R.id.frameLayout, fragmentMinhasVitrines);
            transactionMinhasVitrines.commit();
            fabCadastrarVitrine.show();
            fabExemplo.hide();
            toolbar.setTitle(R.string.minhasVitrines);

        } else if (id == R.id.vitrinesQueSigo) {
            ligaTabsBusca(false);
            ligaTabsMain(false);
            VitrinesSigoFragment fragmentVitrinesSigo = new VitrinesSigoFragment();
            android.support.v4.app.FragmentTransaction transactionVitrines = getSupportFragmentManager().beginTransaction();
            transactionVitrines.replace(R.id.frameLayout, fragmentVitrinesSigo);
            transactionVitrines.commit();
            fabCadastrarVitrine.hide();
            toolbar.setTitle(R.string.vitrinesQueSigo);

        } else if (id == R.id.chat) {
            ligaTabsBusca(false);
            ligaTabsMain(false);
            toolbar.setTitle(R.string.chat);
            ConversasFragment fragmentConversas = new ConversasFragment();
            android.support.v4.app.FragmentTransaction transactionConversas = getSupportFragmentManager().beginTransaction();
            transactionConversas.replace(R.id.frameLayout, fragmentConversas);
            transactionConversas.commit();
            fabCadastrarVitrine.hide();

        } else if (id == R.id.configuracoes) {
            ligaTabsMain(false);
            ConfiguracoesFragment fragmentConfig = new ConfiguracoesFragment();
            FragmentTransaction transactionConfig = getSupportFragmentManager().beginTransaction();
            transactionConfig.replace(R.id.frameLayout, fragmentConfig);
            transactionConfig.commit();
            toolbar.setTitle(R.string.configuracoes);
            fabCadastrarVitrine.hide();

        } else if (id == R.id.entrar) {
            Intent intent = new Intent(this, LoginActivity.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivity(intent,
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            } else {
                startActivity(intent);
            }
            return true;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public boolean EstaLogado() {
        try {
            SharedPreferences prefs = getSharedPreferences("Configuracoes", MODE_PRIVATE);
            if (prefs != null) {
                logado = prefs.getBoolean("logado", false);
                usuario = new Usuario();
                usuario.setNome(prefs.getString("nome", ""));
                usuario.setEmail(prefs.getString("email", ""));
                usuario.setImagemPerfil(prefs.getString("imagemperfil", ""));
                buscaDadosPerfil();
                return logado;
            } else {
                logado = false;
                return logado;
            }
        } catch (NullPointerException exception) {
        }
        return false;
    }

    public void onLogout() {
        SharedPreferences prefs = getSharedPreferences("Configuracoes", MODE_PRIVATE);
        LogOutTask task = new LogOutTask(prefs.getString("email", ""), this);
        task.execute();
    }

    public void ligaTabsMain(boolean liga) {
        if (liga) {
            frameLayout.setVisibility(View.GONE);
            mTabLayout.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.VISIBLE);
            pagerAdapterMain = new PagerAdapterHome(getSupportFragmentManager(), this, numTabs);
            mViewPager.setAdapter(pagerAdapterMain);
            mTabLayout.setupWithViewPager(mViewPager);
        } else {
            frameLayout.setVisibility(View.VISIBLE);
            mTabLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            mViewPager.setAdapter(null);
        }
    }

    public void ligaTabsBusca(boolean liga) {
        if (liga) {
            //Configurando as tabs e fragments
            frameLayout.setVisibility(View.GONE);
            mTabLayout.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.VISIBLE);
            pagerAdapterBusca = new PagerAdapterBuscar(getSupportFragmentManager(), this, numTabs);
            mViewPager.setAdapter(pagerAdapterBusca);
            mTabLayout.setupWithViewPager(mViewPager);
            isBuscaActive = true;
        } else {
            frameLayout.setVisibility(View.VISIBLE);
            mTabLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            mViewPager.setAdapter(null);
            isBuscaActive = false;
        }
    }

    //Metodo para a busca
    public void Pesquisa(String query) {
        ListaResultadoBuscaTask task = new ListaResultadoBuscaTask(this, query);
        task.execute();
    }

    //Metodo para a busca
    public void RecebeLista(List<Vitrine> vitrines) {
        BuscarEventBus event = new BuscarEventBus();
        event.setVitrines(vitrines);
        EventBus.getDefault().post(event);
    }

    public void configuraNavigationView() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.home);

        imagemPerfil = (CircleImageView) navigationView.getHeaderView(0).findViewById(R.id.imageView);
        txtNome = (TextView) navigationView.getHeaderView(0).findViewById(R.id.txtNome);
        txtEmail = (TextView) navigationView.getHeaderView(0).findViewById(R.id.txtEmail);
        //Alterar o conteudo exibido se não logado
        if (!EstaLogado()) {
            navigationView.getMenu().setGroupVisible(R.id.group_logado, false);
            navigationView.getMenu().setGroupVisible(R.id.group_nao_logado, true);
            txtNome.setText(R.string.entre_ou_cadastrese);
            txtEmail.setVisibility(View.GONE);
            imagemPerfil.setVisibility(View.GONE);

        } else {
            Picasso.with(this).load(getResources().getString(R.string.imageservermini) + usuario.getImagemPerfil()).into(imagemPerfil);
            txtNome.setText(usuario.getNome());
            txtEmail.setText(usuario.getEmail());
        }
    }

    public void buscaDadosPerfil() {
        BuscaDadosPerfilTask taskDados = new BuscaDadosPerfilTask(this, usuario);
        taskDados.execute();
    }

    public void recebeDadosPerfil(Usuario usuario) {
        this.usuario = usuario;
        Picasso.with(this).load(getResources().getString(R.string.imageservermini) + usuario.getImagemPerfil()).into(imagemPerfil);
    }

}
