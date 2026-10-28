package com.easyclock.app;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;

public class EasyClockApplication extends Application {

    // Flag de ambiente: true para emuladores locais (10.0.2.2), false para Firebase Cloud (Plano Spark)
    public static final boolean USE_FIREBASE_EMULATORS = false;

    private static final String EMULATOR_HOST = "10.0.2.2";
    private static final int AUTH_EMULATOR_PORT = 9099;
    private static final int FIRESTORE_EMULATOR_PORT = 8080;
    private static final int FUNCTIONS_EMULATOR_PORT = 5001;
    private static final String FUNCTIONS_REGION = "southamerica-east1";

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG && USE_FIREBASE_EMULATORS) {
            configurarEmuladoresFirebase();
        }
    }

    private void configurarEmuladoresFirebase() {
        try {
            FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, AUTH_EMULATOR_PORT);
            FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, FIRESTORE_EMULATOR_PORT);
            FirebaseFunctions.getInstance(FUNCTIONS_REGION).useEmulator(EMULATOR_HOST, FUNCTIONS_EMULATOR_PORT);
        } catch (Exception e) {
            // Emuladores já configurados ou erro de inicialização
        }
    }
}
