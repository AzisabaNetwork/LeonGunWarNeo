package net.azisaba.lgwneo.command;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import java.io.File;
import net.azisaba.lgwneo.LeonGunWarNeo;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TestCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    SlimePlugin plugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
    SlimeLoader sqlLoader = plugin.getLoader("mysql");

    if (args.length == 0) {
      return true;
    }

    if (args[0].equalsIgnoreCase("import")) {
      File worldDir = new File(Bukkit.getWorldContainer(), "test");
      String worldName = "test";
      SlimeLoader loader = plugin.getLoader("mysql");

      LeonGunWarNeo.newChain()
          .async(
              () -> {
                try {
                  plugin.importWorld(worldDir, worldName, loader);
                } catch (Exception ex) {
                  ex.printStackTrace();
                }
              })
          .sync(() -> sender.sendMessage("Imported!"))
          .execute();
    } else if (args[0].equalsIgnoreCase("load")) {
      LeonGunWarNeo.newChain()
          .asyncFirst(
              () -> {
                SlimePropertyMap properties = new SlimePropertyMap();
                properties.setString(SlimeProperties.DIFFICULTY, "normal");
                properties.setInt(SlimeProperties.SPAWN_X, 0);
                properties.setInt(SlimeProperties.SPAWN_Y, 60);
                properties.setInt(SlimeProperties.SPAWN_Z, 0);

                try {
                  return plugin.loadWorld(sqlLoader, "test", true, properties);
                } catch (Exception ex) {
                  ex.printStackTrace();
                }
                return null;
              })
          .abortIfNull()
          .syncLast(plugin::generateWorld)
          .execute();
    }
    return true;
  }
}
