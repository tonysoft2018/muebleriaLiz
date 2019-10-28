//----------------------------------------------------------------------------------------------
// Copyright (c) 2013 Technology Solutions UK Ltd. All rights reserved.
//----------------------------------------------------------------------------------------------

package src.com.uk.tsl.rfid.samples.inventory;

import src.com.uk.tsl.rfid.DeviceListActivity;
import src.com.uk.tsl.rfid.ModelBase;
import src.com.uk.tsl.rfid.WeakHandler;
import src.com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import src.com.uk.tsl.rfid.asciiprotocol.DeviceProperties;
import src.com.uk.tsl.rfid.asciiprotocol.commands.FactoryDefaultsCommand;
import src.com.uk.tsl.rfid.asciiprotocol.device.ConnectionState;
import src.com.uk.tsl.rfid.asciiprotocol.device.IAsciiTransport;
import src.com.uk.tsl.rfid.asciiprotocol.device.ObservableReaderList;
import src.com.uk.tsl.rfid.asciiprotocol.device.Reader;
import src.com.uk.tsl.rfid.asciiprotocol.device.ReaderManager;
import src.com.uk.tsl.rfid.asciiprotocol.device.TransportType;
import src.com.uk.tsl.rfid.asciiprotocol.enumerations.QuerySession;
import src.com.uk.tsl.rfid.asciiprotocol.enumerations.TriState;
import src.com.uk.tsl.rfid.asciiprotocol.parameters.AntennaParameters;
import src.com.uk.tsl.rfid.asciiprotocol.responders.LoggerResponder;
import src.com.uk.tsl.utils.Observable;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.Timer;
import java.util.TimerTask;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import static com.uk.tsl.rfid.DeviceListActivity.EXTRA_DEVICE_ACTION;
import static com.uk.tsl.rfid.DeviceListActivity.EXTRA_DEVICE_INDEX;

public class InventoryActivity extends AppCompatActivity
{
    // Debugging
    private static final String TAG = "InventoryActivity";
	private static final boolean D = BuildConfig.DEBUG;

    // The list of results from actions
    private ArrayAdapter<String> mResultsArrayAdapter;
    private ListView mResultsListView;
    private ArrayAdapter<String> mBarcodeResultsArrayAdapter;
    private ListView mBarcodeResultsListView;

	// The text view to display the RF Output Power used in RFID commands
	private TextView mPowerLevelTextView;
	// The seek bar used to adjust the RF Output Power for RFID commands
	private SeekBar mPowerSeekBar;
	// The current setting of the power level
	private int mPowerLevel = AntennaParameters.MaximumCarrierPower;

	// Error report
	private TextView mResultTextView;

	private TextView txt_Estatus;
	private Button bt_Validar;
    private TextView lbl_Listado;
    private ImageView img_ML;
    private EditText txt_Usuarios2;
    private EditText txt_Contrasenia2;
    private Button bt_Ingresar2;


    // Constantes
    //private String WEBSERVICES_DIRECCION="http://10.20.4.101/gem/";
    private String WEBSERVICES_DIRECCION="http://137.129.171.122:8080/gem/";
    private String WEBSERVICES_VALIDARUSUARIO="validar_usuario.php";
    private String WEBSERVICES_VALIDARECP="validar_ecp.php";
    private String WEBSERVICES_GET_IDVERIFICADOR="get_idverificador.php";

    // Variables globales
    //private static final String TAG = "HttpRequestActivity";

    private String USUARIO="tonysoft";
    private String CONTRASENIA="1";
    private int ID_VERIFICADOR=0;
    private String ECP="";

    private String Resultado="";

    private String Sitio = "";

    private String LEIDO="";

    private String Anterior= "";

    private int BAND_VALIDADO=0;
    private int BAND_LEIDO=0;

    private int CONTADOR=0;

	// Custom adapter for the session values to display the description rather than the toString() value
	public class SessionArrayAdapter extends ArrayAdapter<QuerySession> {
		private final QuerySession[] mValues;

		public SessionArrayAdapter(Context context, int textViewResourceId, QuerySession[] objects) {
			super(context, textViewResourceId, objects);
			mValues = objects;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView)super.getView(position, convertView, parent);
			view.setText(mValues[position].getDescription());
			return view;
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView)super.getDropDownView(position, convertView, parent);
			view.setText(mValues[position].getDescription());
			return view;
		}
	}
	
	// The session
	private QuerySession[] mSessions = new QuerySession[] {
			QuerySession.SESSION_0,
			QuerySession.SESSION_1,
			QuerySession.SESSION_2,
			QuerySession.SESSION_3
	};
    // The list of sessions that can be selected
    private SessionArrayAdapter mSessionArrayAdapter;

	// All of the reader inventory tasks are handled by this class
	private InventoryModel mModel;

    // The Reader currently in use
    private Reader mReader = null;
    private boolean mIsSelectingReader = false;

    //----------------------------------------------------------------------------------------------
	// OnCreate life cycle
	//----------------------------------------------------------------------------------------------

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        Inicializar();


    }

	public void Inicializar(){

        mGenericModelHandler = new GenericHandler(this);

        mResultsArrayAdapter = new ArrayAdapter<String>(this,R.layout.result_item);
        mBarcodeResultsArrayAdapter = new ArrayAdapter<String>(this,R.layout.result_item);

        //mResultTextView = (TextView)findViewById(R.id.resultTextView);

        lbl_Listado =(TextView)findViewById(R.id.lbl_Listado);

        // Find and set up the results ListView
        mResultsListView = (ListView) findViewById(R.id.resultListView);
        mResultsListView.setAdapter(mResultsArrayAdapter);
        mResultsListView.setFastScrollEnabled(true);


        mBarcodeResultsListView = (ListView) findViewById(R.id.barcodeListView);
        mBarcodeResultsListView.setAdapter(mBarcodeResultsArrayAdapter);
        mBarcodeResultsListView.setFastScrollEnabled(true);

        // Hook up the button actions
        Button sButton = (Button)findViewById(R.id.scanButton);
        sButton.setOnClickListener(mScanButtonListener);
        Button cButton = (Button)findViewById(R.id.clearButton);
        cButton.setOnClickListener(mClearButtonListener);

        // The SeekBar provides an integer value for the antenna power
        mPowerLevelTextView = (TextView)findViewById(R.id.powerTextView);
        mPowerSeekBar = (SeekBar)findViewById(R.id.powerSeekBar);
        mPowerSeekBar.setOnSeekBarChangeListener(mPowerSeekBarListener);

        txt_Estatus=(TextView)findViewById(R.id.lbl_Estatus);
        bt_Validar=(Button)findViewById(R.id.bt_Validar);
        bt_Validar.setVisibility(View.GONE);

        img_ML=(ImageView)findViewById(R.id.img_ML);
        bt_Ingresar2=(Button)findViewById(R.id.bt_Ingresar2);
        txt_Usuarios2=(EditText) findViewById(R.id.txt_Usuario2);
        txt_Contrasenia2=(EditText) findViewById(R.id.txt_Contrasenia2);


        mSessionArrayAdapter = new SessionArrayAdapter(this, android.R.layout.simple_spinner_item, mSessions);
        // Find and set up the sessions spinner
        Spinner spinner = (Spinner) findViewById(R.id.sessionSpinner);
        mSessionArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(mSessionArrayAdapter);
        spinner.setOnItemSelectedListener(mActionSelectedListener);
        spinner.setSelection(0);

        // Set up Fast Id check box listener
        CheckBox cb = (CheckBox)findViewById(R.id.fastIdCheckBox);
        cb.setOnClickListener(mFastIdCheckBoxListener);

        // Ensure the shared instance of AsciiCommander exists
        AsciiCommander.createSharedInstance(getApplicationContext());

        AsciiCommander commander = getCommander();

        // Ensure that all existing responders are removed
        commander.clearResponders();

        // Add the LoggerResponder - this simply echoes all lines received from the reader to the log
        // and passes the line onto the next responder
        // This is added first so that no other responder can consume received lines before they are logged.
        commander.addResponder(new LoggerResponder());

        // Add a synchronous responder to handle synchronous commands
        commander.addSynchronousResponder();

        // Create the single shared instance for this ApplicationContext
        ReaderManager.create(getApplicationContext());

        // Add observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().addObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().addObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().addObserver(mRemovedObserver);

        //Create a (custom) model and configure its commander and handler
        mModel = new InventoryModel();
        mModel.setCommander(getCommander());
        mModel.setHandler(mGenericModelHandler);


        mResultsListView.setVisibility(View.GONE);
        txt_Estatus.setVisibility(View.GONE);
        mPowerSeekBar.setVisibility(View.GONE);
        lbl_Listado.setVisibility(View.GONE);
        mPowerLevelTextView.setVisibility(View.GONE);


    }

    public void fn_Ingresar(View view){
	    try{
            String Retorno = "";
            String estatus="NO";

            USUARIO="";
            CONTRASENIA="";

            USUARIO=txt_Usuarios2.getText().toString();
            CONTRASENIA=txt_Contrasenia2.getText().toString();

            if(USUARIO.length()<=0 && CONTRASENIA.length()<=0) {
                Toast.makeText(this, "Usuario y password invalidos" + Sitio, Toast.LENGTH_SHORT).show();
            } else {
                Sitio = WEBSERVICES_DIRECCION +  WEBSERVICES_VALIDARUSUARIO + "?usuario=" + USUARIO + "&password=" + CONTRASENIA;

                try{
                    new FetchTask().execute();

                    Thread.sleep(1000);

                    if(Resultado.length()>0){
                        Retorno=Resultado;
                        Resultado="";
                        new FetchTask().cancel(true);

                        if(Retorno.equals("CORRECTO")){
                            Toast.makeText(this, "Bienvenido",Toast.LENGTH_SHORT).show();



                            mResultsListView.setVisibility(View.VISIBLE);

                            txt_Estatus.setVisibility(View.VISIBLE);

                            mPowerSeekBar.setVisibility(View.VISIBLE);
                            lbl_Listado.setVisibility(View.VISIBLE);
                            mPowerLevelTextView.setVisibility(View.VISIBLE);

                            img_ML.setVisibility(View.GONE);
                            bt_Ingresar2.setVisibility(View.GONE);
                            txt_Usuarios2.setVisibility(View.GONE);
                            txt_Contrasenia2.setVisibility(View.GONE);


                        } else {
                            Toast.makeText(this, "Usuario Invalido",Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "SIN RESPUESTA DEL SERVIDOR",Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e){
                    Toast.makeText(this, "Error:" + e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e){
            Toast.makeText(this, "Error:" + e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    public void reanudar(View view){

        BAND_VALIDADO=0;

        onResume();

        txt_Estatus.setText("Reiniciando");
/*
        AsciiCommander.createSharedInstance(getApplicationContext());

        AsciiCommander commander = getCommander();

        // Ensure that all existing responders are removed
        commander.clearResponders();

        // Add the LoggerResponder - this simply echoes all lines received from the reader to the log
        // and passes the line onto the next responder
        // This is added first so that no other responder can consume received lines before they are logged.
        commander.addResponder(new LoggerResponder());

        // Add a synchronous responder to handle synchronous commands
        commander.addSynchronousResponder();

        // Create the single shared instance for this ApplicationContext
        ReaderManager.create(getApplicationContext());

        // Add observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().addObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().addObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().addObserver(mRemovedObserver);

        //Create a (custom) model and configure its commander and handler
        mModel = new InventoryModel();
        mModel.setCommander(getCommander());
        mModel.setHandler(mGenericModelHandler);
        */

    }

	public void Buscar(){
        txt_Estatus.setText(LEIDO);
        BAND_VALIDADO=1;
        BAND_LEIDO=1;
        bt_Validar.setVisibility(View.VISIBLE);

        /*
        try{
            Thread.sleep(1000);
        } catch(Exception e){

        }
        */


        //EjecutarBusqueda();
    }

    public void EjecutarBusqueda(){

        if(BAND_LEIDO==1){
            String Retorno = "";
            String estatus="NO";
            BAND_LEIDO=0;

            bt_Validar.setVisibility(View.GONE);

            //txt_Estatus.setText("");

            ECP=txt_Estatus.getText().toString();

            Toast.makeText(this,"Lectura:" + ECP,Toast.LENGTH_SHORT).show();

            if(ECP.length()<=0) {
                Toast.makeText(this, "TAG INVALIDO",Toast.LENGTH_SHORT).show();
            } else {
                Sitio = WEBSERVICES_DIRECCION +  WEBSERVICES_VALIDARECP + "?usuario=" + USUARIO + "&password=" + CONTRASENIA + "&ecp="+ECP;
                //Toast.makeText(this, "sitio:" + Sitio,Toast.LENGTH_SHORT).show();


                try{
                    new FetchTask().execute();

                    Thread.sleep(1000);

                    if(Resultado.length()>0){
                        Retorno=Resultado;
                        Resultado="";
                        new FetchTask().cancel(true);

                        if(Retorno.equals("CORRECTO")){
                            txt_Estatus.setText(ECP + " , " + Retorno);
                        } else {
                            txt_Estatus.setText(ECP + " , " + Retorno);
                        }

                        estatus="SI";
                        BAND_VALIDADO=0;
                    } else {
                        txt_Estatus.setText(ECP + " , " + "SIN RESPUESTA DEL SERVIDOR");
                        bt_Validar.setVisibility(View.VISIBLE);
                    }

                } catch (Exception e){
                    Toast.makeText(this, "Error:" + e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    public void Validar(View view){

	    if(BAND_LEIDO==1){
            String Retorno = "";
            String estatus="NO";
            BAND_LEIDO=0;

            bt_Validar.setVisibility(View.GONE);

            //txt_Estatus.setText("");

            ECP=txt_Estatus.getText().toString();

            Toast.makeText(this,"Lectura:" + ECP,Toast.LENGTH_SHORT).show();

            if(ECP.length()<=0) {
                Toast.makeText(this, "TAG INVALIDO",Toast.LENGTH_SHORT).show();
            } else {
                Sitio = WEBSERVICES_DIRECCION +  WEBSERVICES_VALIDARECP + "?usuario=" + USUARIO + "&password=" + CONTRASENIA + "&ecp="+ECP;
                //Toast.makeText(this, "sitio:" + Sitio,Toast.LENGTH_SHORT).show();


                try{
                    new FetchTask().execute();

                    Thread.sleep(1000);

                    if(Resultado.length()>0){
                        Retorno=Resultado;
                        Resultado="";
                        new FetchTask().cancel(true);

                        if(Retorno.equals("CORRECTO")){
                            txt_Estatus.setText(ECP + " , " + Retorno);
                        } else {
                            txt_Estatus.setText(ECP + " , " + Retorno);
                        }

                        estatus="SI";
                        BAND_VALIDADO=0;
                    } else {
                        txt_Estatus.setText(ECP + " , " + "SIN RESPUESTA DEL SERVIDOR");
                        bt_Validar.setVisibility(View.VISIBLE);
                    }

                } catch (Exception e){
                    Toast.makeText(this, "Error:" + e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        }


    }

    /*
    public void Validar(View view){

        if(BAND_LEIDO==1){
            String Retorno = "";
            String estatus="NO";
            BAND_LEIDO=0;

            bt_Validar.setVisibility(View.GONE);

            //txt_Estatus.setText("");

            ECP=txt_Estatus.getText().toString();

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

                            txt_Estatus.setText(ECP + " , " + Retorno);
                        } else {
                            //Toast.makeText(this, "INCORRECTO",Toast.LENGTH_SHORT).show();
                            txt_Estatus.setText(ECP + " , " + Retorno);
                        }

                        BAND_VALIDADO=0;
                    }


                } catch (Exception e){
                    Toast.makeText(this, "Error:" + e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        }


    }
    */


    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // Remove observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().removeObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().removeObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().removeObserver(mRemovedObserver);
    }


    //----------------------------------------------------------------------------------------------
	// Pause & Resume life cycle
	//----------------------------------------------------------------------------------------------

    @Override
    public synchronized void onPause() {
        super.onPause();

        mModel.setEnabled(false);

        // Unregister to receive notifications from the AsciiCommander
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCommanderMessageReceiver);

        // Disconnect from the reader to allow other Apps to use it
        // unless pausing when USB device attached or using the DeviceListActivity to select a Reader
        if( !mIsSelectingReader && !ReaderManager.sharedInstance().didCauseOnPause() && mReader != null )
        {
            mReader.disconnect();
        }

        ReaderManager.sharedInstance().onPause();
    }

    @Override
    public synchronized void onResume() {
    	super.onResume();

        mModel.setEnabled(true);

        // Register to receive notifications from the AsciiCommander
        LocalBroadcastManager.getInstance(this).registerReceiver(mCommanderMessageReceiver, new IntentFilter(AsciiCommander.STATE_CHANGED_NOTIFICATION));

        // Remember if the pause/resume was caused by ReaderManager - this will be cleared when ReaderManager.onResume() is called
        boolean readerManagerDidCauseOnPause = ReaderManager.sharedInstance().didCauseOnPause();

        // The ReaderManager needs to know about Activity lifecycle changes
        ReaderManager.sharedInstance().onResume();

        // The Activity may start with a reader already connected (perhaps by another App)
        // Update the ReaderList which will add any unknown reader, firing events appropriately
        ReaderManager.sharedInstance().updateList();

        // Locate a Reader to use when necessary
        AutoSelectReader(!readerManagerDidCauseOnPause);

        mIsSelectingReader = false;

        displayReaderState();
        UpdateUI();
    }


    //----------------------------------------------------------------------------------------------
    // ReaderList Observers
    //----------------------------------------------------------------------------------------------
    Observable.Observer<Reader> mAddedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            // See if this newly added Reader should be used
            AutoSelectReader(true);
        }
    };

    Observable.Observer<Reader> mUpdatedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
        }
    };

    Observable.Observer<Reader> mRemovedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            mReader = null;
            // Was the current Reader removed
            if( reader == mReader)
            {
                mReader = null;

                // Stop using the old Reader
                getCommander().setReader(mReader);
            }
        }
    };


    private void AutoSelectReader(boolean attemptReconnect)
    {
        ObservableReaderList readerList = ReaderManager.sharedInstance().getReaderList();
        Reader usbReader = null;
        if( readerList.list().size() >= 1)
        {
            // Currently only support a single USB connected device so we can safely take the
            // first CONNECTED reader if there is one
            for (Reader reader : readerList.list())
            {
                IAsciiTransport transport = reader.getActiveTransport();
                if (reader.hasTransportOfType(TransportType.USB))
                {
                    usbReader = reader;
                    break;
                }
            }
        }

        if( mReader == null )
        {
            if( usbReader != null )
            {
                // Use the Reader found, if any
                mReader = usbReader;
                getCommander().setReader(mReader);
            }
        }
        else
        {
            // If already connected to a Reader by anything other than USB then
            // switch to the USB Reader
            IAsciiTransport activeTransport = mReader.getActiveTransport();
            if ( activeTransport != null && activeTransport.type() != TransportType.USB && usbReader != null)
            {
                mReader.disconnect();

                mReader = usbReader;

                // Use the Reader found, if any
                getCommander().setReader(mReader);
            }
        }

        // Reconnect to the chosen Reader
        if( mReader != null && (mReader.getActiveTransport()== null || mReader.getActiveTransport().connectionStatus().value() == ConnectionState.DISCONNECTED))
        {
            // Attempt to reconnect on the last used transport unless the ReaderManager is cause of OnPause (USB device connecting)
            if( attemptReconnect )
            {
                if( mReader.allowMultipleTransports() || mReader.getLastTransportType() == null )
                {
                    // Reader allows multiple transports or has not yet been connected so connect to it over any available transport
                    mReader.connect();
                }
                else
                {
                    // Reader supports only a single active transport so connect to it over the transport that was last in use
                    mReader.connect(mReader.getLastTransportType());
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
	// Menu
	//----------------------------------------------------------------------------------------------

    private MenuItem mConnectMenuItem;
    private MenuItem mDisconnectMenuItem;
	private MenuItem mResetMenuItem;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.reader_menu, menu);

		mResetMenuItem = menu.findItem(R.id.reset_reader_menu_item);
        mConnectMenuItem = menu.findItem(R.id.connect_reader_menu_item);
        mDisconnectMenuItem= menu.findItem(R.id.disconnect_reader_menu_item);
		return true;
	}


	/**
	 * Prepare the menu options
	 */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        boolean isConnecting = getCommander().getConnectionState() == ConnectionState.CONNECTING;
        boolean isConnected = getCommander().isConnected();
        mDisconnectMenuItem.setEnabled(isConnected);

        mConnectMenuItem.setEnabled(true);
        mConnectMenuItem.setTitle( (mReader != null && mReader.isConnected() ? R.string.change_reader_menu_item_text : R.string.connect_reader_menu_item_text));

        mResetMenuItem.setEnabled(isConnected);

        return super.onPrepareOptionsMenu(menu);
    }
    
	/**
	 * Respond to menu item selections
	 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        switch (id) {

            case R.id.reset_reader_menu_item:
                resetReader();
                UpdateUI();
                return true;

            case R.id.connect_reader_menu_item:
                // Launch the DeviceListActivity to see available Readers
                mIsSelectingReader = true;
                int index = -1;
                if( mReader != null )
                {
                    index = ReaderManager.sharedInstance().getReaderList().list().indexOf(mReader);
                }
                Intent selectIntent = new Intent(this, DeviceListActivity.class);
                if( index >= 0 )
                {
                    selectIntent.putExtra(EXTRA_DEVICE_INDEX, index);
                }
                startActivityForResult(selectIntent, DeviceListActivity.SELECT_DEVICE_REQUEST);
                UpdateUI();
                return true;

            case R.id.disconnect_reader_menu_item:
                if( mReader != null )
                {
                    mReader.disconnect();
                    mReader = null;
                    displayReaderState();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //----------------------------------------------------------------------------------------------
	// Model notifications
	//----------------------------------------------------------------------------------------------

    private  class GenericHandler extends WeakHandler<InventoryActivity>
    {
        public GenericHandler(InventoryActivity t)
        {
            super(t);
        }

        @Override
        public void handleMessage(Message msg, InventoryActivity t)
        {
			try {
				switch (msg.what) {
				case ModelBase.BUSY_STATE_CHANGED_NOTIFICATION:
					//TODO: process change in model busy state
					break;

				case ModelBase.MESSAGE_NOTIFICATION:
					// Examine the message for prefix
					String message = (String)msg.obj;
					if( message.startsWith("ER:")) {
						t.mResultTextView.setText( message.substring(3));


					}
					else if( message.startsWith("BC:")) {
							t.mBarcodeResultsArrayAdapter.add(message);
							t.scrollBarcodeListViewToBottom();

					} else {
                        //t.mResultsArrayAdapter.clear();

                        //t.mResultsArrayAdapter.add(message);
                        t.mResultsArrayAdapter.add(message);
                        t.scrollResultsListViewToBottom();

                        LEIDO=message.toString();

                        if(BAND_VALIDADO==0){
                            Buscar();
                        }


                        //t.mResultTextView.setText( message );

						//t.txt_Estatus.setText(message.toString());
					}
                    t.UpdateUI();
					break;
					
				default:
					break;
				}
			} catch (Exception e) {
			}
			
		}
	};

    // The handler for model messages
    private static GenericHandler mGenericModelHandler;

    //----------------------------------------------------------------------------------------------
	// UI state and display update
	//----------------------------------------------------------------------------------------------

    private void displayReaderState() {

        String connectionMsg = "Reader: ";
        switch( getCommander().getConnectionState())
        {
            case CONNECTED:
                connectionMsg += getCommander().getConnectedDeviceName();
                break;
            case CONNECTING:
                connectionMsg += "Connecting...";
                break;
            default:
                connectionMsg += "Disconnected";
        }
        setTitle(connectionMsg);
    }
	
    
    //
    // Set the state for the UI controls
    //
    private void UpdateUI() {
    	//boolean isConnected = getCommander().isConnected();
    	//TODO: configure UI control state
    }


    private void scrollResultsListViewToBottom() {
    	mResultsListView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
            	mResultsListView.setSelection(mResultsArrayAdapter.getCount() - 1);
            }
        });
    }

    private void scrollBarcodeListViewToBottom() {
    	mBarcodeResultsListView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
            	mBarcodeResultsListView.setSelection(mBarcodeResultsArrayAdapter.getCount() - 1);
            }
        });
    }

	
    //----------------------------------------------------------------------------------------------
	// AsciiCommander message handling
	//----------------------------------------------------------------------------------------------

    /**
     * @return the current AsciiCommander
     */
    protected AsciiCommander getCommander()
    {
        return AsciiCommander.sharedInstance();
    }

    //
    // Handle the messages broadcast from the AsciiCommander
    //
    private BroadcastReceiver mCommanderMessageReceiver = new BroadcastReceiver() {
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		if (D) { Log.d(getClass().getName(), "AsciiCommander state changed - isConnected: " + getCommander().isConnected()); }
    		
    		String connectionStateMsg = intent.getStringExtra(AsciiCommander.REASON_KEY);

            displayReaderState();
            if( getCommander().isConnected() )
            {
            	// Update for any change in power limits
                setPowerBarLimits();
                // This may have changed the current power level setting if the new range is smaller than the old range
                // so update the model's inventory command for the new power value
    			mModel.getCommand().setOutputPower(mPowerLevel);
    			
            	mModel.resetDevice();
                mModel.updateConfiguration();
            }

            UpdateUI();
    	}
    };

    //----------------------------------------------------------------------------------------------
	// Reader reset
	//----------------------------------------------------------------------------------------------

    //
    // Handle reset controls
    //
    private void resetReader() {
		try {
			// Reset the reader
			FactoryDefaultsCommand fdCommand = FactoryDefaultsCommand.synchronousCommand();
            fdCommand.setResetParameters(TriState.YES);
			getCommander().executeCommand(fdCommand);

			String msg = "Reset " + (fdCommand.isSuccessful() ? "succeeded" : "failed");
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			
			UpdateUI();

		} catch (Exception e) {
			e.printStackTrace();
		}
    }


	//----------------------------------------------------------------------------------------------
	// Power seek bar
	//----------------------------------------------------------------------------------------------

	//
    // Set the seek bar to cover the range of the currently connected device
	// The power level is set to the new maximum power
	//
    private void setPowerBarLimits()
	{
		DeviceProperties deviceProperties = getCommander().getDeviceProperties();

        mPowerSeekBar.setMax(deviceProperties.getMaximumCarrierPower() - deviceProperties.getMinimumCarrierPower());
        mPowerLevel = deviceProperties.getMaximumCarrierPower();
        mPowerSeekBar.setProgress(mPowerLevel - deviceProperties.getMinimumCarrierPower());
	}


    //
    // Handle events from the power level seek bar. Update the mPowerLevel member variable for use in other actions
    //
    private OnSeekBarChangeListener mPowerSeekBarListener = new OnSeekBarChangeListener() {
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// Nothing to do here
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {

			// Update the reader's setting only after the user has finished changing the value
			updatePowerSetting(getCommander().getDeviceProperties().getMinimumCarrierPower() + seekBar.getProgress());
			mModel.getCommand().setOutputPower(mPowerLevel);
			mModel.updateConfiguration();
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			updatePowerSetting(getCommander().getDeviceProperties().getMinimumCarrierPower() + progress);
		}
	};

	private void updatePowerSetting(int level)	{
		mPowerLevel = level;
		mPowerLevelTextView.setText( mPowerLevel + " dBm");
	}


	//----------------------------------------------------------------------------------------------
	// Button event handlers
	//----------------------------------------------------------------------------------------------

    // Scan action
    private OnClickListener mScanButtonListener = new OnClickListener() {
    	public void onClick(View v) {
    		try {
    			mResultTextView.setText("");
    			// Perform a transponder scan
    			mModel.scan();

    			UpdateUI();

    		} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    };

    // Clear action
    private OnClickListener mClearButtonListener = new OnClickListener() {
    	public void onClick(View v) {
    		try {
    			// Clear the list
    			mResultsArrayAdapter.clear();
    			mBarcodeResultsArrayAdapter.clear();

    			UpdateUI();

    		} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    };
    
	//----------------------------------------------------------------------------------------------
	// Handler for changes in session
	//----------------------------------------------------------------------------------------------

    private AdapterView.OnItemSelectedListener mActionSelectedListener = new AdapterView.OnItemSelectedListener()
    {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			if( mModel.getCommand() != null ) {
				QuerySession targetSession = (QuerySession)parent.getItemAtPosition(pos);
				mModel.getCommand().setQuerySession(targetSession);
				mModel.updateConfiguration();
			}

		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
		}
    };


	//----------------------------------------------------------------------------------------------
	// Handler for changes in FastId
	//----------------------------------------------------------------------------------------------

    private OnClickListener mFastIdCheckBoxListener = new OnClickListener() {
    	public void onClick(View v) {
    		try {
    			CheckBox fastIdCheckBox = (CheckBox)v;
				mModel.getCommand().setUsefastId(fastIdCheckBox.isChecked() ? TriState.YES : TriState.NO);
				mModel.updateConfiguration();
    			
    			UpdateUI();

    		} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    };


    //----------------------------------------------------------------------------------------------
    // Handler for DeviceListActivity
    //----------------------------------------------------------------------------------------------

    //
    // Handle Intent results
    //
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case DeviceListActivity.SELECT_DEVICE_REQUEST:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK)
                {
                    int readerIndex = data.getExtras().getInt(EXTRA_DEVICE_INDEX);
                    Reader chosenReader = ReaderManager.sharedInstance().getReaderList().list().get(readerIndex);

                    int action = data.getExtras().getInt(EXTRA_DEVICE_ACTION);

                    // If already connected to a different reader then disconnect it
                    if( mReader != null )
                    {
                        if( action == DeviceListActivity.DEVICE_CHANGE || action == DeviceListActivity.DEVICE_DISCONNECT)
                        {
                            mReader.disconnect();
                            if(action == DeviceListActivity.DEVICE_DISCONNECT)
                            {
                                mReader = null;
                            }
                        }
                    }

                    // Use the Reader found
                    if( action == DeviceListActivity.DEVICE_CHANGE || action == DeviceListActivity.DEVICE_CONNECT)
                    {
                        mReader = chosenReader;
                        getCommander().setReader(mReader);
                    }
                    displayReaderState();
                }
                break;
        }
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
