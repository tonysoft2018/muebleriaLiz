//----------------------------------------------------------------------------------------------
// Copyright (c) 2013 Technology Solutions UK Ltd. All rights reserved.
//----------------------------------------------------------------------------------------------

package src.com.uk.tsl.rfid.samples.inventory;

import java.util.Locale;

import android.util.Log;

import src.com.uk.tsl.rfid.ModelBase;
import src.com.uk.tsl.rfid.asciiprotocol.commands.BarcodeCommand;
import src.com.uk.tsl.rfid.asciiprotocol.commands.FactoryDefaultsCommand;
import src.com.uk.tsl.rfid.asciiprotocol.commands.InventoryCommand;
import src.com.uk.tsl.rfid.asciiprotocol.enumerations.TriState;
import src.com.uk.tsl.rfid.asciiprotocol.responders.IBarcodeReceivedDelegate;
import src.com.uk.tsl.rfid.asciiprotocol.responders.ICommandResponseLifecycleDelegate;
import src.com.uk.tsl.rfid.asciiprotocol.responders.ITransponderReceivedDelegate;
import src.com.uk.tsl.rfid.asciiprotocol.responders.TransponderData;
import src.com.uk.tsl.utils.HexEncoding;

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



public class InventoryModel extends ModelBase
{

	// Control 
	private boolean mAnyTagSeen;
	private boolean mEnabled;
	public boolean enabled() { return mEnabled; }

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



	private String LEIDO="";
	private String ANTERIOR="";

	public void setEnabled(boolean state)
	{
		boolean oldState = mEnabled;
		mEnabled = state;

		// Update the commander for state changes
		if(oldState != state) {
			if( mEnabled ) {
				// Listen for transponders
				getCommander().addResponder(mInventoryResponder);
				// Listen for barcodes
				getCommander().addResponder(mBarcodeResponder);
			} else {
				// Stop listening for transponders
				getCommander().removeResponder(mInventoryResponder);
				// Stop listening for barcodes
				getCommander().removeResponder(mBarcodeResponder);
			}
			
		}
	}

	// The command to use as a responder to capture incoming inventory responses
	private InventoryCommand mInventoryResponder;
	// The command used to issue commands
	private InventoryCommand mInventoryCommand;

	// The command to use as a responder to capture incoming barcode responses
	private BarcodeCommand mBarcodeResponder;
	
	// The inventory command configuration
	public InventoryCommand getCommand() { return mInventoryCommand; }

	public InventoryModel()
	{
		// This is the command that will be used to perform configuration changes and inventories
		mInventoryCommand = new InventoryCommand();
        mInventoryCommand.setResetParameters(TriState.YES);
		// Configure the type of inventory
		mInventoryCommand.setIncludeTransponderRssi(TriState.YES);
		mInventoryCommand.setIncludeChecksum(TriState.YES);
        mInventoryCommand.setIncludePC(TriState.YES);
        mInventoryCommand.setIncludeDateTime(TriState.YES);

		// Use an InventoryCommand as a responder to capture all incoming inventory responses
		mInventoryResponder = new InventoryCommand();

		// Also capture the responses that were not from App commands 
		mInventoryResponder.setCaptureNonLibraryResponses(true);

		// Notify when each transponder is seen
		mInventoryResponder.setTransponderReceivedDelegate(new ITransponderReceivedDelegate() {

			int mTagsSeen = 0;
			@Override
			public void transponderReceived(TransponderData transponder, boolean moreAvailable) {
				mAnyTagSeen = true;

				String tidMessage = transponder.getTidData() == null ? "" : HexEncoding.bytesToString(transponder.getTidData());
				String infoMsg = String.format(Locale.US, "\nRSSI: %d  PC: %04X  CRC: %04X", transponder.getRssi(), transponder.getPc(), transponder.getCrc());
				//sendMessageNotification("EPC: " + transponder.getEpc() + infoMsg + "\nTID: " + tidMessage + "\n# " + mTagsSeen );

				sendMessageNotification("EPC: " + transponder.getEpc());

				ECP=transponder.getEpc();




				LEIDO=transponder.getEpc().toString();

				mTagsSeen++;
				if( !moreAvailable) {
					sendMessageNotification("");
					Log.d("TagCount",String.format("Tags seen: %s", mTagsSeen));
				}
			}
		});

		mInventoryResponder.setResponseLifecycleDelegate( new ICommandResponseLifecycleDelegate() {
			
			@Override
			public void responseEnded() {
				if( !mAnyTagSeen && mInventoryCommand.getTakeNoAction() != TriState.YES) {
					sendMessageNotification("No transponders seen");
				}
                mInventoryCommand.setTakeNoAction(TriState.NO);
            }
			
			@Override
			public void responseBegan() {
				mAnyTagSeen = false;
			}
		});

		// This command is used to capture barcode responses
		mBarcodeResponder = new BarcodeCommand();
		mBarcodeResponder.setCaptureNonLibraryResponses(true);
		mBarcodeResponder.setUseEscapeCharacter(TriState.YES);
		mBarcodeResponder.setBarcodeReceivedDelegate(new IBarcodeReceivedDelegate() {
			@Override
			public void barcodeReceived(String barcode) {
				sendMessageNotification("BC: " + barcode);
			}
		});

	
	}

	//
	// Reset the reader configuration to default command values
	//
	public void resetDevice()
	{
		if(getCommander().isConnected()) {
            FactoryDefaultsCommand fdCommand = new FactoryDefaultsCommand();
            fdCommand.setResetParameters(TriState.YES);
            getCommander().executeCommand(fdCommand);
		}
	}
	
	//
	// Update the reader configuration from the command
	// Call this after each change to the model's command
	//
	public void updateConfiguration()
	{
		if(getCommander().isConnected()) {
			mInventoryCommand.setTakeNoAction(TriState.YES);
			getCommander().executeCommand(mInventoryCommand);
		}
	}
	
	//
	// Perform an inventory scan with the current command parameters
	//
	public void scan()
	{
		/*
		testForAntenna();
		if(getCommander().isConnected()) {
			mInventoryCommand.setTakeNoAction(TriState.NO);
			getCommander().executeCommand(mInventoryCommand);
		}
		*/

	}


	//
	// Test for the presence of the antenna
	//
	public void testForAntenna()
	{
		if(getCommander().isConnected()) {
			InventoryCommand testCommand = InventoryCommand.synchronousCommand();
			testCommand.setTakeNoAction(TriState.YES);
			getCommander().executeCommand(testCommand);
			if( !testCommand.isSuccessful() ) {
				sendMessageNotification("ER:Error! Code: " + testCommand.getErrorCode() + " " + testCommand.getMessages().toString());
			}
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
