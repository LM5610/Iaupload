package upload.ia;

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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.*;
import java.util.UUID;
public final class Ia_upload extends JavaPlugin {

    private File idFile;
    private FileConfiguration idConfig;

    @Override
    public void onEnable() {
        String[] asciiArt = {
                "|'########:'####:'##::: ##:'##:::'##::'######::'########::|",
                "|... ##..::. ##:: ###:: ##: ##::'##::'##... ##: ##.... ##:|",
                "|::: ##::::: ##:: ####: ##: ##:'##::: ##:::..:: ##:::: ##:|"   + "          Tinksp上传资源包插件"  ,
                "|::: ##::::: ##:: ## ## ##: #####::::. ######:: ########::|"   + "          群号:464570091",
                "|::: ##::::: ##:: ##. ####: ##. ##::::..... ##: ##.....:::|"   + "          网站:https://www.tinksp.cn/",
                "|::: ##::::: ##:: ##:. ###: ##:. ##::'##::: ##: ##::::::::|",
                "|::: ##::::'####: ##::. ##: ##::. ##:. ######:: ##::::::::|",
                "|:::..:::::....::..::::..::..::::..:::......:::..:::::::::|"
        };

        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "——————————————————————————————");
        for (String line : asciiArt) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + line);
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "——————————————————————————————");

        // 创建/加载 id.yml 文件
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

        // 检查是否已经有一个 userId
        if (!idConfig.contains("pluginUser.userId")) {
            //生成一个新的 UUID 并保存到 id.yml
            String newUserId = UUID.randomUUID().toString();
            idConfig.set("pluginUser.userId", newUserId);
            try {
                idConfig.save(idFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.getCommand("iaupload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (!sender.isOp()) {
                    sender.sendMessage("你没有权限执行这个命令！");
                    return true;
                }

                if (args.length > 0 && args[0].equalsIgnoreCase("run")) {
                    File file = new File("plugins/ItemsAdder/output/generated.zip");
                    if (file.exists()) {
                        uploadFileAsync(file, sender);
                    } else {
                        sender.sendMessage("文件不存在！");
                    }
                }
                return true;
            }
        });
    }

    // 将文件上传逻辑放到一个异步任务中
    private void uploadFileAsync(File file, CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> uploadFile(file, sender));
    }

    public void uploadFile(File file, CommandSender sender) {

        // 从 id.yml 文件中读取 userId
        String userId = idConfig.getString("pluginUser.userId");

        String randomChars = UUID.randomUUID().toString().substring(0, 5);
        String newFileName = userId + "_" + randomChars + ".zip";

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost uploadFile = new HttpPost("http://ia.tinksp.cn:38031/upload");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", file, ContentType.DEFAULT_BINARY, newFileName);
        HttpEntity multipart = builder.build();

        uploadFile.setEntity(multipart);

        try (CloseableHttpResponse response = httpClient.execute(uploadFile)) {
            HttpEntity responseEntity = response.getEntity();
            String responseString = EntityUtils.toString(responseEntity);

            JSONObject jsonResponse = new JSONObject(responseString);
            if (jsonResponse.getString("status").equals("success")) {
                String downloadUrl = jsonResponse.getString("download_url");

                // 修改ia的config.yml
                modifyConfigFile(downloadUrl);

                sender.sendMessage("上传文件成功啦！现在先输入/iareload在输入/iatexture all全部重新加载吧");
            } else {
                sender.sendMessage("文件上传失败！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage("上传过程中出现错误！");
        }
    }

    public void modifyConfigFile(String newUrl) {
        File configFile = new File(getServer().getPluginManager().getPlugin("ItemsAdder").getDataFolder(), "config.yml");
        StringBuilder newContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("      url: ")) {
                    line = "      url: " + newUrl;  // 替换新的URL
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
