package cn.tinksp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.*;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class Tinkia extends JavaPlugin {

    private File idFile;
    private FileConfiguration idConfig;

    @Override
    public void onEnable() {
        String[] asciiArt = {
                "|'########:'####:'##::: ##:'##:::'##::'######::'########::|",
                "|... ##..::. ##:: ###:: ##: ##::'##::'##... ##: ##.... ##:|",
                "|::: ##::::: ##:: ####: ##: ##:'##::: ##:::..:: ##:::: ##:|"   + "          Tinksp上传资源包插件"  ,
                "|::: ##::::: ##:: ## ## ##: #####::::. ######:: ########::|"   + "          群号:464570091",
                "|::: ##::::: ##:: ##. ####: ##. ##::::..... ##: ##.....:::|"   + "          网站:https://www.tinksp.com/",
                "|::: ##::::: ##:: ##:. ###: ##:. ##::'##::: ##: ##::::::::|",
                "|::: ##::::'####: ##::. ##: ##::. ##:. ######:: ##::::::::|",
                "|:::..:::::....::..::::..::..::::..:::......:::..:::::::::|"
        };

        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "——————————————————————————————");
        for (String line : asciiArt) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + line);
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "——————————————————————————————");


        idFile = new File(getDataFolder(), "id.yml");
        if (!idFile.exists()) {
            idFile.getParentFile().mkdirs();
            saveResource("id.yml", false);
        }

        idConfig = new YamlConfiguration();
        try {
            idConfig.load(idFile);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (!idConfig.contains("pluginUser.userId")) {

            String newUserId = UUID.randomUUID().toString();
            idConfig.set("pluginUser.userId", newUserId);
            try {
                idConfig.save(idFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

       
        this.getCommand("iaupload").setExecutor((sender, command, label, args) -> {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行这个命令");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("run")) {
                try {
                    
                    File interfaceJson = createInterfaceJson();

                    
                    addFileToZip(interfaceJson, "assets/minecraft/Interface/Interface.json");

                   
                    File zipFile = new File("plugins/ItemsAdder/output/generated.zip");
                    if (zipFile.exists()) {
                        uploadFileAsync(zipFile, sender);
                    } else {
                        sender.sendMessage(ChatColor.RED + "文件不存在！");
                    }
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "处理文件时发生错误！");
                    e.printStackTrace();
                }
            }
            return true;
        });
    }




    private File createInterfaceJson() throws IOException {
        
        File interfaceJson = new File(getDataFolder(), "Interface.json");
        if (!interfaceJson.exists()) {
            interfaceJson.getParentFile().mkdirs();
            interfaceJson.createNewFile();
        }

        
        String jsonContent = "{\"interface\": \"用于防止恶意上传\"}";
        try (FileWriter writer = new FileWriter(interfaceJson)) {
            writer.write(jsonContent);
        }
        return interfaceJson;
    }

    private void addFileToZip(File sourceFile, String entryName) throws IOException {
        File tempFile = File.createTempFile("temp_generated", ".zip");
        tempFile.delete();

        File originalZip = new File("plugins/ItemsAdder/output/generated.zip");
        boolean entryExists = false;

        try (ZipFile zipFile = new ZipFile(originalZip);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {
            for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                if (entry.getName().equals(entryName)) {
                    entryExists = true; 
                }
                
                zos.putNextEntry(new ZipEntry(entry.getName()));
                try (InputStream is = zipFile.getInputStream(entry)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        zos.write(buf, 0, len);
                    }
                }
                zos.closeEntry();
            }

            
            if (!entryExists) {
                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);
                try (FileInputStream fis = new FileInputStream(sourceFile)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = fis.read(buf)) > 0) {
                        zos.write(buf, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }

        
        if (!originalZip.delete()) {
            throw new IOException(" "); 
        }
        if (!tempFile.renameTo(originalZip)) {
            throw new IOException(" "); 
        }
    }

    private void uploadFileAsync(File file, CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                
                String uploadResponse = uploadFile(file);

               
                if (uploadResponse != null) {
                    
                    Bukkit.getScheduler().runTask(this, () -> {
                        modifyConfigFile(uploadResponse);
                        sender.sendMessage("上传文件成功啦！现在先输入/iareload 再输入/iatexture all全部重新加载吧");
                    });
                } else {
                    
                    sender.sendMessage("文件上传失败!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                sender.sendMessage("上传过程中出现错误!");
            }
        });
    }

    private String uploadFile(File file) throws IOException {
        String userId = idConfig.getString("pluginUser.userId");
        String randomChars = UUID.randomUUID().toString().substring(0, 5);
        String newFileName = userId + "_" + randomChars + ".zip";

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost uploadFile = new HttpPost("http://ia.tinksp.cn/upload");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", file, ContentType.DEFAULT_BINARY, newFileName);
        HttpEntity multipart = builder.build();

        uploadFile.setEntity(multipart);

        CloseableHttpResponse response = httpClient.execute(uploadFile);
        HttpEntity responseEntity = response.getEntity();
        String responseString = EntityUtils.toString(responseEntity);

        JSONObject jsonResponse = new JSONObject(responseString);
        if (jsonResponse.getString("status").equals("success")) {
            return jsonResponse.getString("download_url");
        } else {
            return null;
        }
    }

    public void modifyConfigFile(String newUrl) {
        File configFile = new File(getServer().getPluginManager().getPlugin("ItemsAdder").getDataFolder(), "config.yml");
        StringBuilder newContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("      url: ")) {
                    line = "      url: " + newUrl;
                }
                newContent.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            writer.write(newContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
