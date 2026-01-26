package com.council.availabilityservice.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Configuration
public class GoogleCalendarConfig {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    @Value("${google.calendar.credentials.file.path}")
    private Resource credentialsFile;

    @Value("${google.calendar.tokens.directory.path}")
    private String tokensDirectoryPath;

    @Value("${google.calendar.application.name}")
    private String applicationName;

    @Value("${google.calendar.redirect.uri}")
    private String redirectUri;

    /**
     * Creates an authorized Credential object for a specific user.
     *
     * @param userId The user ID (counselor ID)
     * @return An authorized Credential object
     * @throws IOException If the credentials.json file cannot be found
     */
    public Credential getCredentials(final NetHttpTransport httpTransport, String userId)
            throws IOException {

        // Load client secrets
        InputStream in = credentialsFile.getInputStream();
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(
                        new java.io.File(tokensDirectoryPath)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        // Use userId as the credential identifier
        return new AuthorizationCodeInstalledApp(flow, receiver)
                .authorize(userId);
    }

    /**
     * Creates a Calendar service instance for a specific user.
     *
     * @param userId The user ID (counselor ID)
     * @return Calendar service instance
     */
    public Calendar getCalendarService(String userId)
            throws GeneralSecurityException, IOException {

        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(httpTransport, userId);

        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();
    }

    /**
     * Bean for creating HTTP transport (can be used for dependency injection)
     */
    @Bean
    public NetHttpTransport netHttpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }

    /**
     * Bean for JSON factory
     */
    @Bean
    public JsonFactory jsonFactory() {
        return JSON_FACTORY;
    }
}