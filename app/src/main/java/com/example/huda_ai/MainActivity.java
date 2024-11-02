package com.example.huda_ai;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itextpdf.text.DocumentException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.text.DocumentException;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    String lastQuestion;
    ImageButton btnAction;
    ImageButton btnAgain;
    ImageButton btnDownload;
    EditText editTextText;
    List<Message> messageList;
    RecyclerView rv;
    MessageAdapter messageAdapter;
    StringBuilder conversationHistory = new StringBuilder();

    private static final int PERMISSION_REQUEST_CODE = 100;

    //public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        messageList = new ArrayList<>();

        btnAction = findViewById(R.id.btnAction);
        editTextText = findViewById(R.id.editTextText);
        rv = findViewById(R.id.rv);
        btnAgain = findViewById(R.id.btnAgain);

        messageAdapter = new MessageAdapter(messageList);
        rv.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(getApplicationContext());
        llm.setStackFromEnd(true);
        rv.setLayoutManager(llm);
        firstquestion(getString(R.string.firstquestion));

        btnAgain.setVisibility(View.INVISIBLE);

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question = editTextText.getText().toString().trim();
                addToChat(question, Message.SENT_BY_ME);
                editTextText.setText("");
                modelCall(question);
            }
        });

        btnAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToChat(lastQuestion, Message.SENT_BY_ME);
                modelCall(lastQuestion);
            }
        });


        /*
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkUserPermission();

            }

            public void checkUserPermission() {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(new MainActivity(), new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                } else {
                    // Permission granted, proceed with your action
                }
            }
        });
        */



        editTextText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    btnAction.setVisibility(View.VISIBLE);
                    btnAgain.setVisibility(View.INVISIBLE);
                } else {
                    btnAction.setVisibility(View.INVISIBLE);
                    btnAgain.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });



    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            rv.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SENT_BY_BOT);
    }





    public void firstquestion(String firstquestion){
        conversationHistory.append(firstquestion).append("\n");

        messageList.add(new Message("Yazıyor...", Message.SENT_BY_BOT));

        String fullContext = conversationHistory.toString();

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", "AIzaSyCE0XJfXBIf4vFIFNlrbHzkeSkWBFZAiyk");
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder().addText(fullContext).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();

                // Append the response to the conversation history
                conversationHistory.append(resultText).append("\n");
                addResponse(resultText);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                addResponse("Error: Unable to generate response.");
            }
        }, this.getMainExecutor());

    }


    public void modelCall(String question) {
        lastQuestion = question;

        conversationHistory.append(question).append("\n");

        messageList.add(new Message("Yazıyor...", Message.SENT_BY_BOT));

        String fullContext = conversationHistory.toString();

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", "AIzaSyCE0XJfXBIf4vFIFNlrbHzkeSkWBFZAiyk");
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder().addText(fullContext).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();


                /*
                SpannableString spannableString = new SpannableString(resultText);
                Pattern pattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
                Matcher matcher = pattern.matcher(resultText);
                while (matcher.find()) {
                    int start = matcher.start(1);
                    int end = matcher.end(1);
                    spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                addResponse(spannableString);
                */


                // Append the response to the conversation history
                conversationHistory.append(resultText).append("\n");
                addResponse(resultText);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                addResponse("Error: Unable to generate response.");
            }
        }, this.getMainExecutor());
    }
}