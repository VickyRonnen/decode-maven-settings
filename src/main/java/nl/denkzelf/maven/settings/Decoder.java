package nl.denkzelf.maven.settings;

import org.apache.commons.cli.*;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

import org.apache.maven.settings.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author Jelmer Kuperus
 * @Coaythor Vicky Ronnen
 */
public class Decoder {

    public static void main(String... args) throws Exception {

        String settingsFileName = System.getProperty("user.home") + "/.m2/settings.xml";
        String securityFileName = System.getProperty("user.home") + "/.m2/settings-security.xml";

        File settingsFile = new File(settingsFileName);
        File securityFile = new File(securityFileName);
        printPasswords(settingsFile, securityFile);
    }

    private static String decodePassword(String encodedPassword, String key) throws PlexusCipherException {
        DefaultPlexusCipher cipher = new DefaultPlexusCipher();
        return cipher.decryptDecorated(encodedPassword, key);
    }

    private static String decodeMasterPassword(String encodedMasterPassword) throws PlexusCipherException {
        return decodePassword(encodedMasterPassword, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
    }

    private static SettingsSecurity readSettingsSecurity(File file) throws SecDispatcherException {
        return SecUtil.read(file.getAbsolutePath(), true);
    }

    private static Settings readSettings(File file) throws IOException, XmlPullParserException {
        SettingsXpp3Reader reader = new SettingsXpp3Reader();
        return reader.read(new FileInputStream(file));
    }

    private static void printPasswords(File settingsFile, File securityFile)
            throws IOException, XmlPullParserException, SecDispatcherException, PlexusCipherException {

        Settings settings = readSettings(settingsFile);
        SettingsSecurity settingsSecurity = readSettingsSecurity(securityFile);

        String encodedMasterPassword = settingsSecurity.getMaster();
        String plainTextMasterPassword = decodeMasterPassword(encodedMasterPassword);

        System.out.printf("Master password is : %s%n", plainTextMasterPassword);
        List<Server> servers = settings.getServers();

        for (Server server : servers) {
            String encodedServerPassword = server.getPassword();
            String plainTextServerPassword = decodePassword(encodedServerPassword, plainTextMasterPassword);

            System.out.println("-------------------------------------------------------------------------");
            System.out.printf("Credentials for server %s are :%n", server.getId());
            System.out.printf("Username : %s%n", server.getUsername());
            System.out.printf("Password : %s%n", plainTextServerPassword);
        }

    }
}
