openPDS-Client
=============

Library for integrating Android applications with openPDS

This library provides support for interfacing with openPDS Registry and PDS servers from within Android applications.

=============

Getting Started (assuming Eclipse with ADT as a development environment):

0) Create a client on the Registry Server you plan to intregrate with. This provides you with a client secret, key, and basic auth string to use in your app. 

1) Pull down the code, and import openPDSClient as a project in your workspace. This should be done within Eclipse by selecting File->New->Other->Android->Android Project From Existing Code. 

2) Add openPDSClient as a project reference in your application. This is done by right-clicking your project, selecting properties from the context menu and performing the following steps:

    a) In "Java Build Path", click on the "Projects" tab, click the "Add..." button and select openPDSClient from the list. Then, from the "Order and Export" tab, check the box next to openPDSClient".  
    b) In "Project References", check the box next to openPDSClient. 
    c) In "Android", click the "Add..." button under the "Library" heading, and select openPDSClient from the list. 

3) For funf integration, you'll need to update your Pipeline config to use "edu.mit.media.openpds.client.funf.OpenPDSPipeline" as the type, and this config must be included as meta-data on the FunfManager service in your Android Manifest. After doing this, and adding entries for the openPDS/Funf launcher receiver and upload service, your application manifest should have the following entries (in place of the traditional Funf equivalents):

       <service
            android:name="edu.mit.media.funf.FunfManager"
            android:enabled="true" >
            <meta-data
                android:name="MainPipeline"
                android:resource="@string/main_pipeline_config" />
        </service>        
        <service
            android:name="edu.mit.media.openpds.client.funf.HttpsUploadService"
            android:enabled="true" >
        </service>
        <receiver
            android:name="edu.mit.media.openpds.client.funf.LauncherReceiver"
            android:enabled="true" >
            <intent-filter>
                <action android:name="android.intent.action.BATTERY_CHANGED" />
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.DOCK_EVENT" />
                <action android:name="android.intent.action.ACTION_SCREEN_ON" />
                <action android:name="android.intent.action.USER_PRESENT" />
            </intent-filter>
        </receiver>

4) Interfacing with the Registry Server for creating accounts and authorizign access to applications is done through the RegistryClient class, which takes a URL to the Registry Server, a client key and secret, the scopes your application uses, and the Basic Auth string to be used when communicating with the Registry Server. Necessary scopes are determined on an application basis. For most purposes, funf_write is the only one needed. Client key, secret, and basic auth are provided by the Registry Server when you create your client. 

5) Working with a PDS requires that the user is registered and has authorized your app via the registry client. After this is done, Funf integration is complete - the OpenPDSPipeline handles collecting data and uploading to the user's PDS. For storing different types of information the PersonalDataStore class should be extended and used with a connector on the PDS to collec the data. 

An example of each of these steps is provided at: 

https://github.com/HumanDynamics/Roskilde
