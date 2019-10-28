package com.example.mueblerializ;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import android.app.AlertDialog.Builder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import com.uk.tsl.rfid.ModelBase;
import com.uk.tsl.rfid.WeakHandler;
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import com.uk.tsl.rfid.asciiprotocol.DeviceProperties;
import com.uk.tsl.rfid.asciiprotocol.commands.FactoryDefaultsCommand;
import com.uk.tsl.rfid.asciiprotocol.device.ConnectionState;
import com.uk.tsl.rfid.asciiprotocol.device.IAsciiTransport;
import com.uk.tsl.rfid.asciiprotocol.device.ObservableReaderList;
import com.uk.tsl.rfid.asciiprotocol.device.Reader;
import com.uk.tsl.rfid.asciiprotocol.device.ReaderManager;
import com.uk.tsl.rfid.asciiprotocol.device.TransportType;
import com.uk.tsl.rfid.asciiprotocol.enumerations.QuerySession;
import com.uk.tsl.rfid.asciiprotocol.enumerations.TriState;
import com.uk.tsl.rfid.asciiprotocol.parameters.AntennaParameters;
import com.uk.tsl.rfid.asciiprotocol.responders.LoggerResponder;
import com.uk.tsl.utils.Observable;



public class lectura extends AppCompatActivity {

    // Constantes
    private String WEBSERVICES_DIRECCION="http://10.20.4.101/gem/";
    private String WEBSERVICES_VALIDARUSUARIO="validar_usuario.php";
    private String WEBSERVICES_VALIDARECP="validar_ecp.php";
    private String WEBSERVICES_GET_IDVERIFICADOR="get_idverificador.php";

    // Variables globales
    private static final String TAG = "HttpRequestActivity";

    private String USUARIO="";
    private String CONTRASENIA="";
    private int ID_VERIFICADOR=0;
    private String ECP="";

    private String Resultado="";

    private String Sitio = "";

    // Componentes
    private EditText txt_Lectura;
    private TextView lbl_Estatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lectura);

        USUARIO = getIntent().getStringExtra("usuario");
        CONTRASENIA = getIntent().getStringExtra("contrasenia");

        //Toast.makeText(this,"usuario:" + USUARIO + "," + CONTRASENIA,Toast.LENGTH_SHORT).show();

        txt_Lectura=(EditText)findViewById(R.id.txt_Lectura);
        lbl_Estatus=(TextView)findViewById(R.id.lbl_EstatusLectura);
    }


    public void bt_Validar(View view){
        String Retorno = "";
        String estatus="NO";

        lbl_Estatus.setText("");

        ECP="";
        ECP=txt_Lectura.getText().toString();

        Toast.makeText(this,"Lectura:" + ECP,Toast.LENGTH_SHORT).show();

        if(ECP.length()<=0) {
            Toast.makeText(this, "TAG INVALIDO",Toast.LENGTH_SHORT).show();
        } else {
            Sitio = WEBSERVICES_DIRECCION +  WEBSERVICES_VALIDARECP + "?usuario=" + USUARIO + "&password=" + CONTRASENIA + "&ecp="+ECP;
            //Toast.makeText(this, "sitio:" + Sitio,Toast.LENGTH_SHORT).show();


            try{
                new FetchTask().execute();

                while(true){
                    if(Resultado.length()>0){
                        Retorno=Resultado;
                        Resultado="";
                        new FetchTask().cancel(true);

                        if(Retorno.equals("CORRECTO")){
                            //Toast.makeText(this, "CORRECTO",Toast.LENGTH_SHORT).show();
                        } else {
                            //Toast.makeText(this, "INCORRECTO",Toast.LENGTH_SHORT).show();
                        }

                        estatus="SI";
                        break;
                    }
                }

                if(estatus.equals("SI")==true){
                    if(Retorno.equals("REVISION EXITOSA")){
                        //Toast.makeText(this, "CORRECTO",Toast.LENGTH_SHORT).show();

                        lbl_Estatus.setText(ECP + " , " + Retorno);
                    } else {
                        //Toast.makeText(this, "INCORRECTO",Toast.LENGTH_SHORT).show();
                        lbl_Estatus.setText(ECP + " , " + Retorno);
                    }
                }


            } catch (Exception e){
                Toast.makeText(this, "Error:" + e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Botones
    public void bt_Finalizar(View view){
        System.exit(0);
    }



    // --------------------------------------
    private class FetchTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Show progressbar
            //setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected String doInBackground(Void... params) {

            try {

                URL url = new URL(Sitio);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Log the server response code
                int responseCode = connection.getResponseCode();
                Log.i(TAG, "Server responded with: " + responseCode);

                // And if the code was HTTP_OK then parse the contents
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    // Convert request content to string
                    InputStream is = connection.getInputStream();
                    String content = convertInputStream(is, "UTF-8");
                    is.close();

                    Resultado=content;

                    return content;
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        private String convertInputStream(InputStream is, String encoding) {
            Scanner scanner = new Scanner(is, encoding).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

    }
    // --------------------------------------


}
