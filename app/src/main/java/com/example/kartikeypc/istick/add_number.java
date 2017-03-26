package com.example.kartikeypc.istick;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by KartikeyPC on 14-03-2017.
 */
public class add_number extends Dialog {

    Context context;
    File path,file;
    EditText ip_add, port_no;
    Button btn_save,btn_clear;
    List<String> num_list = new ArrayList<>();
    String ip_address,port;

    public add_number(Context context) {
        super(context);
        this.context = context;
        path = context.getFilesDir();
        file = new File(path, "Phone_Number.txt");
        if(file.exists())
        {
            readFromFile();

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_number);
        findViewById(R.id.RL).getBackground().setAlpha(250);
        btn_save = (Button) findViewById(R.id.btn_save);
        btn_clear = (Button) findViewById(R.id.btn_clear);
        ip_add = (EditText) findViewById(R.id.txt_ip);
        ip_add.setText(ip_address);
        port_no = (EditText) findViewById(R.id.txt_port);
        port_no.setText(port);
        ip_address = ip_add.getText().toString();
        port = port_no.getText().toString();
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ip_address = ip_add.getText().toString();
                port = port_no.getText().toString();
                // Log.i("Parth Connection", "Trying to write number " + port );

                writeToFile(ip_address,port);
                dismiss();
            }
        });
        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearFile();
            }
        });
    }

    public List<String> getNum_list()
    {
        readFromFile();
        return num_list;
    }


    private void writeToFile(String ip, String port) {
        try {

            FileWriter writer = new FileWriter(file,true);
            writer.write(ip+";"+port+"\n");
            writer.close();
            //Log.i("Parth Connection", "Writtten to file " );

        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }



    private void readFromFile(){


       // int length = (int) file.length();

       // byte[] bytes = new byte[length];
        String str = "";
        StringBuffer buf = new StringBuffer();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            num_list.clear();
            if (in != null) {
                while ((str = reader.readLine()) != null) {
                    String[] arr = str.split(";");
                    num_list.add(arr[1]);
                  //  Log.i("Parth Connection", "Read from file " + arr[1] );
                    // buf.append(str + "\n" );
                }
            }
           // in.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //String contents = new String(bytes);


       // return buf.toString();//contents;
    }

    private void clearFile() {
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.print("");
            writer.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }



}
