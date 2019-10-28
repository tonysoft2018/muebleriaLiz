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

public class MainActivity extends AppCompatActivity {

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
    private EditText txt_Usuario;
    private EditText txt_Contrasenia;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt_Usuario=(EditText)findViewById(R.id.txt_Usuario);
        txt_Contrasenia=(EditText)findViewById(R.id.txt_Contrasenia);
    }


    // Acciones
    public void fn_UsuarioValido(){
        Intent lectura = new Intent(this, lectura.class);
        lectura.putExtra("usuario", USUARIO);
        lectura.putExtra("contrasenia", CONTRASENIA);
        startActivity(lectura);
    }

    public void fn_UsuarioNoValido(){
        Intent denegado = new Intent(this, accesodenegado.class);
        startActivity(denegado);
    }


    // Botones
    public void bt_Ingresar(View view){
        String Retorno = "";
        String estatus="NO";

        USUARIO="";
        CONTRASENIA="";

        USUARIO=txt_Usuario.getText().toString();
        CONTRASENIA=txt_Contrasenia.getText().toString();

        if(USUARIO.length()<=0 && CONTRASENIA.length()<=0){
            Toast.makeText(this, "Usuario y password invalidos" + Sitio,Toast.LENGTH_SHORT).show();
        } else {
            Sitio = WEBSERVICES_DIRECCION +  WEBSERVICES_VALIDARUSUARIO + "?usuario=" + USUARIO + "&password=" + CONTRASENIA;
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
                    if(Retorno.equals("CORRECTO")){
                        fn_UsuarioValido();
                    } else {
                        fn_UsuarioNoValido();
                    }
                }

            } catch (Exception e){
                Toast.makeText(this, "Error:" + e.getMessage(),Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void bt_Configurar(View view){



    }

    public void bt_Cancelar(View view){
        System.exit(0);
        finish();
    }


    // --------------------------------------
    // Pruebas
    public void Prueba(View view){
        Intent prueba = new Intent(this, accesodenegado.class);
        startActivity(prueba);
    }

    public void Prueba2(View view){
        Intent prueba = new Intent(this, lectura.class);
        startActivity(prueba);
    }
    // --------------------------------------

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
