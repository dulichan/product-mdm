/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.emm.agent;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.emm.agent.api.DeviceInfo;
import org.wso2.emm.agent.beans.RegistrationProfile;
import org.wso2.emm.agent.beans.ServerConfig;
import org.wso2.emm.agent.proxy.beans.CredentialInfo;
import org.wso2.emm.agent.proxy.interfaces.APIAccessCallBack;
import org.wso2.emm.agent.proxy.interfaces.APIResultCallBack;
import org.wso2.emm.agent.proxy.utils.Constants.HTTP_METHODS;
import org.wso2.emm.agent.proxy.IdentityProxy;
import org.wso2.emm.agent.services.DynamicClientManager;
import org.wso2.emm.agent.utils.CommonDialogUtils;
import org.wso2.emm.agent.utils.Constants;
import org.wso2.emm.agent.utils.Preference;
import org.wso2.emm.agent.utils.CommonUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Activity that captures username, password and device ownership details 
 * and handles authentication.
 */
public class AuthenticationActivity extends SherlockActivity implements APIAccessCallBack,
                                                                        APIResultCallBack {
	private Button btnRegister;
	private EditText etUsername;
	private EditText etDomain;
	private EditText etPassword;
	private RadioButton radioBYOD;
	private String deviceType;
	private Context context;
	private String username;
	private String usernameVal;
	private String passwordVal;
	private ProgressDialog progressDialog;

	private static final String MIME_TYPE = "text/html";
	private static final String ENCODING_METHOD = "utf-8";
	private static final int DEFAILT_REPEAT_COUNT = 0;
	private static final int NOTIFIER_CHECK = 2;
	public static int DEFAULT_INTERVAL = 30000;
	private DeviceInfo deviceInfo;
	private static final String TAG = AuthenticationActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authentication);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
		getSupportActionBar().setCustomView(R.layout.custom_sherlock_bar);
		getSupportActionBar().setTitle(Constants.EMPTY_STRING);

		context = this;
		deviceInfo = new DeviceInfo(context);
		deviceType = Constants.OWNERSHIP_BYOD;
		etDomain = (EditText) findViewById(R.id.etDomain);
		etUsername = (EditText) findViewById(R.id.etUsername);
		etPassword = (EditText) findViewById(R.id.etPassword);
		radioBYOD = (RadioButton) findViewById(R.id.radioBYOD);
		etDomain.setFocusable(true);
		etDomain.requestFocus();
		btnRegister = (Button) findViewById(R.id.btnRegister);
		btnRegister.setOnClickListener(onClickAuthenticate);
		btnRegister.setEnabled(false);
		btnRegister.setOnClickListener(onClickAuthenticate);
		// change button color background till user enters a valid input
		btnRegister.setBackground(getResources().getDrawable(R.drawable.btn_grey));
		btnRegister.setTextColor(getResources().getColor(R.color.black));

		etUsername.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				enableSubmitIfReady();
			}

			@Override
			public void afterTextChanged(Editable s) {
				enableSubmitIfReady();
			}
		});

		etPassword.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				enableSubmitIfReady();
			}

			@Override
			public void afterTextChanged(Editable s) {
				enableSubmitIfReady();
			}
		});

	}

	private OnClickListener onClickAuthenticate = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (etUsername.getText() != null && !etUsername.getText().toString().trim().isEmpty() &&
			    etPassword.getText() != null && !etPassword.getText().toString().trim().isEmpty()) {

				passwordVal = etPassword.getText().toString().trim();
				usernameVal = etUsername.getText().toString().trim();
				if (etDomain.getText() != null && !etDomain.getText().toString().trim().isEmpty()) {
					usernameVal +=
							getResources().getString(R.string.intent_extra_at) +
							etDomain.getText().toString().trim();
				}

				if (radioBYOD.isChecked()) {
					deviceType = Constants.OWNERSHIP_BYOD;
				} else {
					deviceType = Constants.OWNERSHIP_COPE;
				}
				
				showAuthenticationDialog();			
			} else {
				if (etUsername.getText() != null &&
				    !etUsername.getText().toString().trim().isEmpty()) {
					Toast.makeText(context,
					               getResources().getString(R.string.toast_error_password),
					               Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(context,
					               getResources().getString(R.string.toast_error_username),
					               Toast.LENGTH_LONG).show();
				}
			}

		}
	};

	private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					startAuthentication();
					dialog.dismiss();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					dialog.dismiss();
					break;

				default:
					break;
			}
		}
	};

	/**
	 * Start authentication process.
	 */
	private void startAuthentication() {
		Preference.putString(context, Constants.DEVICE_TYPE,
				deviceType);

		// Check network connection availability before calling the API.
		if (CommonUtils.isNetworkAvailable(context)) {

			String clientId = Preference.getString(context, Constants.CLIENT_ID);
			String clientSecret = Preference.getString(context, Constants.CLIENT_SECRET);
			String clientName;

			progressDialog =
					ProgressDialog.show(context,
							getResources().getString(R.string.dialog_authenticate),
							getResources().getString(R.string.dialog_message_please_wait),
							true);

			if (clientId == null || clientSecret == null) {
				try {
					String clientCredentials = getClientCredentials();
					if (clientCredentials != null) {
						try {
							JSONObject payload = new JSONObject(clientCredentials);
							clientId = payload.getString(Constants.CLIENT_ID);
							clientSecret = payload.getString(Constants.CLIENT_SECRET);
							clientName = payload.getString(Constants.CLIENT_NAME);

							if (clientName != null && !clientName.isEmpty()) {
								Preference.putString(context, Constants.CLIENT_NAME, clientName);
							}
							if (clientId != null && !clientId.isEmpty() &&
									clientSecret != null && !clientSecret.isEmpty()) {
								initializeIDPLib(clientId, clientSecret);
							}
						} catch (JSONException e) {
							String msg = "error occurred while parsing client credential payload";
							Log.e(TAG, msg, e);
							showInternalServerErrorMessage();
						}
					} else {
						String msg = "error occurred while retrieving client credentials";
						Log.e(TAG, msg);
						showInternalServerErrorMessage();
					}
				} catch (AndroidAgentException e) {
					String msg = "error occurred while retrieving client credentials";
					Log.e(TAG, msg, e);
					showInternalServerErrorMessage();
				}
			} else {
				initializeIDPLib(clientId, clientSecret);
			}

		} else {
			CommonDialogUtils.stopProgressDialog(progressDialog);
			CommonDialogUtils.showNetworkUnavailableMessage(context);
		}

	}

	/**
	 * Initialize the Android IDP SDK by passing credentials,client ID and
	 * client secret.
	 *
	 * @param clientKey    client id value to access APIs..
	 * @param clientSecret client secret value to access APIs.
	 */
	private void initializeIDPLib(String clientKey, String clientSecret) {
		
		String serverIP =
				Preference.getString(AuthenticationActivity.this, Constants.IP);
		ServerConfig utils = new ServerConfig();
		utils.setServerIP(serverIP);
		String serverURL = utils.getServerURL() + Constants.OAUTH_ENDPOINT;
		
		if (etDomain.getText() != null && !etDomain.getText().toString().trim().isEmpty()) {
			username =
					etUsername.getText().toString().trim() +
					context.getResources().getString(R.string.intent_extra_at) +
					etDomain.getText().toString().trim();

		} else {
			username = etUsername.getText().toString().trim();
		}

		Preference.putString(context, Constants.CLIENT_ID, clientKey);
		Preference.putString(context, Constants.CLIENT_SECRET, clientSecret);
		
		CredentialInfo info = new CredentialInfo();
		info.setClientID(clientKey);
		info.setClientSecret(clientSecret);
		info.setUsername(username);
		info.setPassword(passwordVal);
		info.setTokenEndPoint(serverURL);
		
		IdentityProxy.getInstance().init(info, AuthenticationActivity.this, this.getApplicationContext());
	}

	@Override
	public void onAPIAccessReceive(String status) {
        if (status != null) {
			if (status.trim().equals(Constants.Status.SUCCESSFUL)) {

				Preference.putString(context, Constants.USERNAME, username);

				// Check network connection availability before calling the API.
				CommonDialogUtils.stopProgressDialog(progressDialog);
				if (CommonUtils.isNetworkAvailable(context)) {
					getLicense();
				} else {
					CommonDialogUtils.stopProgressDialog(progressDialog);
					CommonDialogUtils.showNetworkUnavailableMessage(AuthenticationActivity.this);
				}

			} else if (status.trim().equals(Constants.Status.AUTHENTICATION_FAILED)) {
				showAuthenticationError();
				try {
					CommonUtils.clearAppData(context);
				} catch (AndroidAgentException e) {
					Log.e(TAG, "Failed to clear app data.", e);
				}
			} else if (status.trim().equals(Constants.Status.INTERNAL_SERVER_ERROR)) {
				showInternalServerErrorMessage();

			} else {
				showAuthCommonErrorMessage();
			}

		} else {
			showAuthCommonErrorMessage();
		}

	}

	/**
	 * Initialize get device license agreement. Check if the user has already
	 * agreed to license agreement
	 */
	private void getLicense() {
		String licenseAgreedResponse =
				Preference.getString(context,
				                     getResources().getString(R.string.shared_pref_isagreed));
		String type =
				Preference.getString(context, Constants.DEVICE_TYPE);

		if (Constants.OWNERSHIP_BYOD.equals(type.trim())) {
			if (licenseAgreedResponse == null) {

				OnCancelListener cancelListener = new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface arg0) {
						CommonDialogUtils.getAlertDialogWithOneButtonAndTitle(context,
                              getResources().getString(R.string.error_enrollment_failed_detail),
                              getResources().getString(R.string.error_enrollment_failed),
                              getResources().getString(R.string.button_ok), null);
					}
				};

				progressDialog =
						CommonDialogUtils.showPrgressDialog(context,
						                                    getResources().getString(
								                                    R.string.dialog_license_agreement),
						                                    getResources().getString(
								                                    R.string.dialog_please_wait),
						                                    cancelListener);

				// Check network connection availability before calling the API.
				if (CommonUtils.isNetworkAvailable(context)) {
					getLicenseFromServer();
				} else {
					CommonDialogUtils.stopProgressDialog(progressDialog);
					CommonDialogUtils.showNetworkUnavailableMessage(context);
				}

			} else {
				getConfigurationsFromServer();
			}
		} else {
			getConfigurationsFromServer();
		}

	}

	/**
	 * Retriever license agreement details from the server.
	 */
	private void getLicenseFromServer() {
		String ipSaved =
				Preference.getString(context.getApplicationContext(),Constants.IP);

		ServerConfig utils = new ServerConfig();
		utils.setServerIP(ipSaved);
		CommonUtils.callSecuredAPI(AuthenticationActivity.this,
				utils.getAPIServerURL() + Constants.LICENSE_ENDPOINT,
				HTTP_METHODS.GET, null, AuthenticationActivity.this,
				Constants.LICENSE_REQUEST_CODE
		);
	}

	/**
	 * Retriever configurations from the server.
	 */
	private void getConfigurationsFromServer() {
		OnCancelListener cancelListener = new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface arg0) {
				CommonDialogUtils.getAlertDialogWithOneButtonAndTitle(context,
	                                          getResources().getString(R.string.error_enrollment_failed_detail),
	                                          getResources().getString(R.string.error_enrollment_failed),
	                                          getResources().getString(R.string.button_ok), null);
			}
		};
		progressDialog =
				CommonDialogUtils.showPrgressDialog(context,
				                                    getResources().getString(
						                                    R.string.dialog_sender_id),
				                                    getResources().getString(
						                                    R.string.dialog_please_wait),
				                                    cancelListener);
		String ipSaved =
				Preference.getString(context.getApplicationContext(),Constants.IP);

		ServerConfig utils = new ServerConfig();
		utils.setServerIP(ipSaved);
		CommonUtils.callSecuredAPI(AuthenticationActivity.this,
		                           utils.getAPIServerURL() + Constants.CONFIGURATION_ENDPOINT,
		                           HTTP_METHODS.GET, null, AuthenticationActivity.this,
		                           Constants.CONFIGURATION_REQUEST_CODE
		);
	}

	@Override
	public void onReceiveAPIResult(Map<String, String> result, int requestCode) {
		if (requestCode == Constants.LICENSE_REQUEST_CODE) {
			manipulateLicenseResponse(result);
		} else if(requestCode == Constants.CONFIGURATION_REQUEST_CODE){
			manipulateConfigurationResponse(result);
		}
	}
	/**
	 * Manipulates the Configuration response received from server.
	 *
	 * @param result the result of the configuration request
	 */
	private void manipulateConfigurationResponse(Map<String, String> result) {
		String responseStatus;
		CommonDialogUtils.stopProgressDialog(progressDialog);

		if (result != null) {
			responseStatus = result.get(Constants.STATUS);
			if (Constants.Status.SUCCESSFUL.equals(responseStatus)) {
				String configurationResponse = result.get(Constants.RESPONSE);

				if (configurationResponse != null) {
					try {
						JSONObject config = new JSONObject(configurationResponse.toString());
						if (!config.isNull(context.getString(R.string.shared_pref_configuration))) {
							JSONArray configList = new JSONArray(config.getString(context.getString(R.string.
					                                                                      shared_pref_configuration)));
							for (int i = 0; i < configList.length(); i++) {
								JSONObject param = new JSONObject(configList.get(i).toString());
								if(param.getString(context.getString(R.string.shared_pref_config_key)).trim().equals(
										context.getString(R.string.shared_pref_notifier))){
									String type = param.getString(context.getString(R.string.shared_pref_config_value)).trim();
									if(type.equals(String.valueOf(NOTIFIER_CHECK))) {
										Preference.putString(context, getResources().getString(R.string.shared_pref_notifier),
										                     Constants.NOTIFIER_GCM);
									}else{
										Preference.putString(context, getResources().getString(R.string.shared_pref_notifier),
										                     Constants.NOTIFIER_LOCAL);
									}
								} else if(param.getString(context.getString(R.string.shared_pref_config_key)).trim().
										equals(context.getString(R.string.shared_pref_frequency))){
										Preference.putInt(context, getResources().getString(R.string.shared_pref_frequency),
										                  Integer.valueOf(param.getString(context.getString(R.string.shared_pref_config_value)).trim()));
								} else if(param.getString(context.getString(R.string.shared_pref_config_key)).trim().
										equals(context.getString(R.string.shared_pref_gcm))){
										Preference.putString(context, getResources().getString(R.string.shared_pref_sender_id),
									                     param.getString(context.getString(R.string.shared_pref_config_value)).trim());
								}
							}
						}

					} catch (JSONException e) {
						Log.e(TAG, "Error parsing configuration response JSON", e);
						setDefaultNotifier();
					}
				} else {
					Log.e(TAG, "Empty configuration response");
					setDefaultNotifier();
				}

			} else if (Constants.Status.INTERNAL_SERVER_ERROR.equals(responseStatus)) {
				Log.e(TAG, "Empty configuration response.");
				setDefaultNotifier();
			} else {
				Log.e(TAG, "Empty configuration response.");
				setDefaultNotifier();
			}

		} else {
			Log.e(TAG, "Empty configuration response.");
			setDefaultNotifier();
		}
		loadNextActivity();
	}

	private void setDefaultNotifier(){
		Preference.putString(context, getResources().getString(R.string.shared_pref_notifier),
		                     Constants.NOTIFIER_LOCAL);
		Preference.putInt(context, getResources().getString(R.string.shared_pref_frequency),
		                    DEFAULT_INTERVAL);
	}


	/**
	 * Manipulates the License agreement response received from server.
	 *
	 * @param result the result of the license agreement request
	 */
	private void manipulateLicenseResponse(Map<String, String> result) {
		String responseStatus;
		CommonDialogUtils.stopProgressDialog(progressDialog);

		if (result != null) {
			responseStatus = result.get(Constants.STATUS);
			if (Constants.Status.SUCCESSFUL.equals(responseStatus)) {
                		String licenseAgreement = result.get(Constants.RESPONSE);

				if (licenseAgreement != null) {
					Preference.putString(context,
					                     getResources().getString(R.string.shared_pref_eula),
					                     licenseAgreement);
					showAgreement(licenseAgreement, Constants.EULA_TITLE);
				} else {
					showErrorMessage(
							getResources().getString(R.string.error_enrollment_failed_detail),
							getResources().getString(R.string.error_enrollment_failed));
				}

			} else if (Constants.Status.INTERNAL_SERVER_ERROR.equals(responseStatus)) {
				showInternalServerErrorMessage();
			} else {
				showEnrollementFailedErrorMessage();
			}

		} else {
			showEnrollementFailedErrorMessage();
		}
	}
	
	private void showAuthenticationDialog(){
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder.append(getResources().getString(R.string.dialog_init_middle));
		messageBuilder.append(getResources().getString(R.string.intent_extra_space));
		messageBuilder.append(deviceType);
		messageBuilder.append(getResources().getString(R.string.intent_extra_space));
		messageBuilder.append(getResources().getString(R.string.dialog_init_end));
		AlertDialog.Builder alertDialog =
				CommonDialogUtils.getAlertDialogWithTwoButtonAndTitle(context,
						getResources().getString(R.string.dialog_init_device_type),
						messageBuilder.toString(),
						getResources().getString(R.string.yes),
						getResources().getString(R.string.no),
						dialogClickListener, dialogClickListener);
		alertDialog.show();
	}

	private void showErrorMessage(String message, String title) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message);
		builder.setTitle(title);
		builder.setCancelable(true);
		builder.setPositiveButton(getResources().getString(R.string.button_ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						cancelEntry();
						dialog.dismiss();
					}
				}
		);
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Show the license text retrieved from the server.
	 *
	 * @param message Message text to be shown as the license.
	 * @param title   Title of the license.
	 */
	private void showAgreement(String message, String title) {
		final Dialog dialog = new Dialog(context);
		dialog.setContentView(R.layout.custom_terms_popup);
		dialog.setTitle(Constants.EULA_TITLE);
		dialog.setCancelable(false);

		WebView webView = (WebView) dialog.findViewById(R.id.webview);

		webView.loadDataWithBaseURL(null, message, MIME_TYPE, ENCODING_METHOD, null);

		Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
		Button cancelButton = (Button) dialog.findViewById(R.id.dialogButtonCancel);

		dialogButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Preference.putString(context,
						getResources().getString(R.string.shared_pref_isagreed),
						getResources().getString(R.string.shared_pref_reg_success));
				dialog.dismiss();
				//load the next intent based on ownership type
				getConfigurationsFromServer();
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				cancelEntry();
			}
		});

		dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_SEARCH &&
						event.getRepeatCount() == DEFAILT_REPEAT_COUNT) {
					return true;
				} else if (keyCode == KeyEvent.KEYCODE_BACK &&
						event.getRepeatCount() == DEFAILT_REPEAT_COUNT) {
					return true;
				}
				return false;
			}
		});

		dialog.show();
	}

	private void loadPinCodeActivity() {
		Intent intent = new Intent(AuthenticationActivity.this, PinCodeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(Constants.USERNAME, usernameVal);
		startActivity(intent);
	}

	private void loadRegistrationActivity() {
		Intent intent = new Intent(AuthenticationActivity.this, RegistrationActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(Constants.USERNAME, usernameVal);
		startActivity(intent);
	}

	private void cancelEntry() {

		Preference.putString(context, getResources().getString(R.string.shared_pref_policy),
		                     getResources().getString(R.string.shared_pref_default_string));
		Preference.putString(context, getResources().getString(R.string.shared_pref_isagreed),
		                     getResources().getString(R.string.shared_pref_reg_fail));
		Preference.putString(context, getResources().getString(R.string.shared_pref_registered),
		                     getResources().getString(R.string.shared_pref_reg_fail));
		Preference.putString(context, getResources().getString(R.string.shared_pref_ip),
		                     getResources().getString(R.string.shared_pref_default_string));

		Intent intentIP = new Intent(AuthenticationActivity.this, ServerDetails.class);
		intentIP.putExtra(getResources().getString(R.string.intent_extra_from_activity),
		                  AuthenticationActivity.class.getSimpleName());
		intentIP.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intentIP);

	}

	/**
	 * Validation done to see if the username and password fields are properly
	 * entered.
	 */
	private void enableSubmitIfReady() {

		boolean isReady = false;

		if (etUsername.getText().toString().length() >= 1 &&
		    etPassword.getText().toString().length() >= 1) {
			isReady = true;
		}

		if (isReady) {
			btnRegister.setBackground(getResources().getDrawable(R.drawable.btn_orange));
			btnRegister.setTextColor(getResources().getColor(R.color.white));
			btnRegister.setEnabled(true);
		} else {
			btnRegister.setBackground(getResources().getDrawable(R.drawable.btn_grey));
			btnRegister.setTextColor(getResources().getColor(R.color.black));
			btnRegister.setEnabled(false);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.auth_sherlock_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ip_setting:
				Preference.putString(context, Constants.IP,Constants.EMPTY_STRING);

				Intent intentIP = new Intent(AuthenticationActivity.this, ServerDetails.class);
				intentIP.putExtra(getResources().getString(R.string.intent_extra_from_activity),
				                  AuthenticationActivity.class.getSimpleName());
				startActivity(intentIP);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent();
			i.setAction(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_HOME);
			this.startActivity(i);
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_HOME) {
			this.finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private DialogInterface.OnClickListener senderIdFailedClickListener =
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,
				                    int which) {
					etUsername.setText(Constants.EMPTY_STRING);
					etPassword.setText(Constants.EMPTY_STRING);
					etDomain.setText(Constants.EMPTY_STRING);
					btnRegister.setEnabled(false);
					btnRegister.setBackground(getResources().getDrawable(R.drawable.btn_grey));
					btnRegister.setTextColor(getResources().getColor(R.color.black));
				}
			};

	/**
	 * Shows enrollment failed error.
	 */		
	private void showEnrollementFailedErrorMessage() {
		CommonDialogUtils.stopProgressDialog(progressDialog);
		CommonDialogUtils.getAlertDialogWithOneButtonAndTitle(context,
                      getResources().getString(
                              R.string.error_enrollment_failed),
                      getResources().getString(
                              R.string.error_enrollment_failed_detail),
                      getResources().getString(
                              R.string.button_ok),
                      senderIdFailedClickListener);
	}

	/**
	 * Shows internal server error message for authentication.
	 */
	private void showInternalServerErrorMessage() {
		CommonDialogUtils.stopProgressDialog(progressDialog);
		CommonDialogUtils.getAlertDialogWithOneButtonAndTitle(context,
                      getResources().getString(
                              R.string.title_head_connection_error),
                      getResources().getString(
                              R.string.error_internal_server),
                      getResources().getString(
                              R.string.button_ok),
                      null);
	}
	
	/**
	 * Shows credentials error message for authentication.
	 */
	private void showAuthenticationError(){
		CommonDialogUtils.stopProgressDialog(progressDialog);
		CommonDialogUtils.getAlertDialogWithOneButtonAndTitle(context,
              getResources().getString(R.string.title_head_authentication_error),
              getResources().getString(R.string.error_authentication_failed),
              getResources().getString(R.string.button_ok),
              null);
	}

	/**
	 * Shows common error message for authentication.
	 */
	private void showAuthCommonErrorMessage() {
		CommonDialogUtils.stopProgressDialog(progressDialog);
		CommonDialogUtils.getAlertDialogWithOneButtonAndTitle(context,
                      getResources().getString(
                              R.string.title_head_authentication_error),
                      getResources().getString(
                              R.string.error_for_all_unknown_authentication_failures),
                      getResources().getString(
                              R.string.button_ok),
                      null);

	}

	/**
	 * This method is used to retrieve consumer-key and consumer-secret.
	 *
	 * @return JSON formatted string.
	 * @throws AndroidAgentException
	 */
	private String getClientCredentials() throws AndroidAgentException {
		String ipSaved = Preference.getString(context.getApplicationContext(), Constants.IP);
		ServerConfig utils = new ServerConfig();
		utils.setServerIP(ipSaved);

		RegistrationProfile profile = new RegistrationProfile();
		profile.setCallbackUrl(Constants.EMPTY_STRING);
		profile.setClientName(deviceInfo.getDeviceId());
		profile.setGrantType(Constants.GRANT_TYPE);
		profile.setOwner(usernameVal);
		profile.setTokenScope(Constants.TOKEN_SCOPE);

		DynamicClientManager dynamicClientManager = new DynamicClientManager();
		return dynamicClientManager.getClientCredentials(profile, utils);
	}

	/**
	 * This method is used to bypass the intents based on the
	 * ownership type.
	 */
	private void loadNextActivity() {
		if (Constants.OWNERSHIP_BYOD.equalsIgnoreCase(deviceType)) {
			loadPinCodeActivity();
		} else {
			loadRegistrationActivity();
		}
	}
}
