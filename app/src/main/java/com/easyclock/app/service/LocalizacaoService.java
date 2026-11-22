package com.easyclock.app.service;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

public class LocalizacaoService {

    public static final float RAIO_PERMITIDO_PADRAO_METROS = 100.0f;
    public static final float ACCURACY_THRESHOLD_METROS = 50.0f;
    public static final long MAX_LOCATION_AGE_MS = 60000L; // 60 segundos

    public interface OnLocalizacaoCapturadaListener {
        void onSuccess(Location location, float precisaoMetros, boolean isMock);
        void onFailure(String mensagemErro, boolean gpsDesativado, boolean permissaoNegada);
    }

    public static boolean temPermissaoLocalizacao(Context context) {
        if (context == null) return false;
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean gpsHabilitado(Context context) {
        if (context == null) return false;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return false;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public static boolean isMockLocation(Location location) {
        if (location == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return location.isMock();
        } else {
            return location.isFromMockProvider();
        }
    }

    public static float calcularDistanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    public static void capturarLocalizacaoAtual(Activity activity, OnLocalizacaoCapturadaListener listener) {
        if (activity == null || listener == null) return;

        if (!temPermissaoLocalizacao(activity)) {
            listener.onFailure("Permissão de localização precisa (GPS) não concedida. Habilite a localização nas configurações do aplicativo.", false, true);
            return;
        }

        if (!gpsHabilitado(activity)) {
            listener.onFailure("GPS desativado no dispositivo. Por favor, ative a localização para continuar.", true, false);
            return;
        }

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(activity);
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                    .addOnSuccessListener(activity, location -> {
                        if (location != null) {
                            validarEProcessarLocalizacao(location, activity, fusedClient, listener);
                        } else {
                            // Tenta obter lastLocation como fallback se não estiver desatualizada
                            fusedClient.getLastLocation().addOnSuccessListener(activity, lastLoc -> {
                                if (lastLoc != null) {
                                    validarEProcessarLocalizacao(lastLoc, activity, fusedClient, listener);
                                } else {
                                    listener.onFailure("Não foi possível identificar sua localização. Verifique o GPS e tente novamente.", false, false);
                                }
                            }).addOnFailureListener(e -> {
                                listener.onFailure("Não foi possível obter a localização do dispositivo: " + e.getLocalizedMessage(), false, false);
                            });
                        }
                    })
                    .addOnFailureListener(activity, e -> {
                        listener.onFailure("Falha ao obter localização atual: " + e.getLocalizedMessage(), false, false);
                    });
        } catch (SecurityException se) {
            listener.onFailure("Erro de permissão ao acessar localização: " + se.getLocalizedMessage(), false, true);
        }
    }

    private static void validarEProcessarLocalizacao(Location location, Activity activity, FusedLocationProviderClient fusedClient, OnLocalizacaoCapturadaListener listener) {
        if (isMockLocation(location)) {
            listener.onFailure("Localização simulada detectada. O registro de ponto foi bloqueado.", false, false);
            return;
        }

        long idadeMs = System.currentTimeMillis() - location.getTime();
        if (idadeMs > MAX_LOCATION_AGE_MS) {
            // Localização antiga, solicitar nova leitura
            listener.onFailure("Sua localização está desatualizada. Por favor, aguarde alguns segundos e tente novamente.", false, false);
            return;
        }

        float precisao = location.hasAccuracy() ? location.getAccuracy() : 999.0f;
        if (precisao > ACCURACY_THRESHOLD_METROS) {
            listener.onFailure(String.format("A precisão da sua localização está baixa (%.0fm). Tente novamente em um local aberto.", precisao), false, false);
            return;
        }

        listener.onSuccess(location, precisao, false);
    }
}
