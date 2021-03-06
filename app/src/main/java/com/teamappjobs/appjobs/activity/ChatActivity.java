package com.teamappjobs.appjobs.activity;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.teamappjobs.appjobs.R;
import com.teamappjobs.appjobs.adapter.RecyclerViewAdapterChat;
import com.teamappjobs.appjobs.asyncTask.EnviaMensagemTask;
import com.teamappjobs.appjobs.asyncTask.ReceberMensagensTask;
import com.teamappjobs.appjobs.modelo.Mensagem;
import com.teamappjobs.appjobs.modelo.Vitrine;

import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends ActionBarActivity {

    private Toolbar toolbar;
    private EditText txtMensagem;
    private RecyclerView recyclerViewMensagens;
    private FloatingActionButton fabEnviar;
    private String remetente;
    private String destinatario;
    private Vitrine vitrine;
    private LinearLayoutManager mLayoutManager;
    private String profPicDestinatario="";
    private String nomeDestinatario;

    private int page = 1;
    private int pageV = 0;


    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerViewAdapterChat adapter;
    private List<Mensagem> mensagens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences prefs = getSharedPreferences("Configuracoes", MODE_PRIVATE);

        Intent intent = getIntent();
        String origem = intent.getStringExtra("origem");
        vitrine= (Vitrine) intent.getSerializableExtra("vitrine");

        if (origem.equals("faleComOAnunciante")) {
            destinatario = vitrine.getEmailAnunciante();
            remetente = prefs.getString("email", "");
            profPicDestinatario = intent.getStringExtra("fotoAnunciante");
            nomeDestinatario = intent.getStringExtra("nomeAnunciante");
        }

        if (origem.equals("notificacao")) {
            remetente = prefs.getString("email", "");
            destinatario = intent.getStringExtra("emailRemetente");
            profPicDestinatario = intent.getStringExtra("profPicRemetente");
            nomeDestinatario = intent.getStringExtra("nomeRemetente");
        }
        if (origem.equals("icNav")) {
            remetente = prefs.getString("email", "");
            destinatario = intent.getStringExtra("emailRemetente");
            profPicDestinatario = intent.getStringExtra("profPicRemetente");
            nomeDestinatario = intent.getStringExtra("nomeRemetente");
        }


        //Configurando a toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        // LayoutPersonalizado
        LayoutInflater mInflater = LayoutInflater.from(this);
        View mCustomView = mInflater.inflate(R.layout.toolbar_chat_layout, null);
        CircleImageView imagemDestinatario = (CircleImageView) mCustomView.findViewById(R.id.circularImageView);
        if (!profPicDestinatario.equals("")) {
            Picasso.with(this).load(this.getResources().getString(R.string.imageservermini) + profPicDestinatario).into(imagemDestinatario);
        }
        TextView txtNomeDestinatario = (TextView) mCustomView.findViewById(R.id.title_text);
        txtNomeDestinatario.setText(nomeDestinatario);

        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(mCustomView);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        txtMensagem = (EditText) findViewById(R.id.mensagem);
        recyclerViewMensagens = (RecyclerView) findViewById(R.id.recycler_view_mensagens);

        mLayoutManager = new LinearLayoutManager(this);
        recyclerViewMensagens.setLayoutManager(mLayoutManager);
        recyclerViewMensagens.setItemAnimator(new DefaultItemAnimator());
        recyclerViewMensagens.setHasFixedSize(true);


        fabEnviar = (FloatingActionButton) findViewById(R.id.fabEnviar);
        fabEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!txtMensagem.getText().toString().equals("")) {

                    Mensagem mensagem = new Mensagem();
                    mensagem.setMsg(txtMensagem.getText().toString());
                    Date dataAgora = new Date();
                    mensagem.setDthrEnviada(dataAgora);
                    mensagem.setCodMsg(0);
                    mensagem.setRemetente(remetente);
                    mensagem.setDestinatario(destinatario);
                    mensagens.add(mensagem);

                    adapter = new RecyclerViewAdapterChat(ChatActivity.this, mensagens, remetente);
                    recyclerViewMensagens.setAdapter(adapter);
                    adapter.setSelectedItem(adapter.getItemCount() - 1);
                    recyclerViewMensagens.scrollToPosition(adapter.getItemCount() - 1);

                    String auxString = txtMensagem.getText().toString();
                    EnviaMensagemTask task = new EnviaMensagemTask(remetente, destinatario, auxString, ChatActivity.this);
                    task.execute();

                    txtMensagem.setText("");

                }
            }
        });


        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.wallet_holo_blue_light);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i("AtualizandoMsgs", "onRefresh called from SwipeRefreshLayout");
                page++;

                receberMensagens();
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();

        //CancelaNotification se houver
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(destinatario, 0);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("Chat", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("chatAtivo", true);
        editor.putString("chatDestinatario", destinatario);
        editor.commit();
        receberMensagens();

        //Ligar reeeiver
        this.registerReceiver(mMessageReceiver, new IntentFilter("chat"));

    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences("Chat", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("chatAtivo", false);
        editor.putString("chatDestinatario", "");
        editor.commit();


        //Desligar Receiver
        this.unregisterReceiver(mMessageReceiver);
    }


    public void receberMensagens() {

        ReceberMensagensTask mensagensTask = new ReceberMensagensTask(remetente, destinatario, this, page);
        mensagensTask.execute();

    }

    public void atualizarTela(List<Mensagem> mensagens) {

        this.mensagens = mensagens;
        adapter = new RecyclerViewAdapterChat(this, mensagens, remetente);
        recyclerViewMensagens.setAdapter(adapter);
        adapter.setSelectedItem(adapter.getItemCount() - 1);
        recyclerViewMensagens.scrollToPosition(adapter.getItemCount() - 1);

    }




    public void updateRecyclerView(List<Mensagem> mensagens){

        if(page != pageV) {
            this.mensagens.addAll(0, mensagens);
            adapter.notifyDataSetChanged();
            recyclerViewMensagens.scrollToPosition(mensagens.size() + 1);
            mSwipeRefreshLayout.setRefreshing(false);

            pageV = page;
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            receberMensagens();
        }
    };
}
