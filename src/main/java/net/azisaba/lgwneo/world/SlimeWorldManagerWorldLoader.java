package net.azisaba.lgwneo.world;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.util.Chat;
import net.azisaba.lgwneo.util.ServerTransferUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class SlimeWorldManagerWorldLoader implements WorldLoader {

  private final LeonGunWarNeo plugin;

  private final SlimePlugin slimePlugin;
  private final SlimeLoader slimeLoader;

  @Override
  public List<String> getAvailableWorlds() {
    try {
      List<String> worlds = new ArrayList<>(slimeLoader.listWorlds());
      worlds.removeIf(name -> Bukkit.getWorld(name) != null);
      return worlds;
    } catch (IOException ex) {
      ex.printStackTrace();
      return Collections.emptyList();
    }
  }

  @Override
  public List<String> getUnavailableWorlds() {
    try {
      List<String> worlds = new ArrayList<>(slimeLoader.listWorlds());
      worlds.removeIf(name -> Bukkit.getWorld(name) == null);
      return worlds;
    } catch (IOException ex) {
      ex.printStackTrace();
      return Collections.emptyList();
    }
  }

  @Override
  public CompletableFuture<World> loadWorld(String mapName, String worldName) {
    if (Bukkit.isPrimaryThread()) {
      throw new IllegalStateException(
          "Cannot call this method on main thread. The primary thread will be stuck.");
    }
    CompletableFuture<World> future = new CompletableFuture<>();

    LeonGunWarNeo.newChain()
        .asyncFirst(
            () -> {
              SlimePropertyMap properties = new SlimePropertyMap();
              properties.setString(SlimeProperties.DIFFICULTY, "normal");
              properties.setInt(SlimeProperties.SPAWN_X, 0);
              properties.setInt(SlimeProperties.SPAWN_Y, 60);
              properties.setInt(SlimeProperties.SPAWN_Z, 0);

              try {
                return slimePlugin
                    .loadWorld(slimeLoader, mapName, true, properties)
                    .clone(worldName);
              } catch (Exception ex) {
                ex.printStackTrace();
                return null;
              }
            })
        .sync(
            slimeWorld -> {
              long start = System.currentTimeMillis();
              slimePlugin.generateWorld(slimeWorld);
              plugin
                  .getLogger()
                  .info(
                      Chat.f(
                          "Generated world {0} in {1}ms",
                          worldName, System.currentTimeMillis() - start));
              return Bukkit.getWorld(slimeWorld.getName());
            })
        .asyncLast(future::complete)
        .execute((exception, task) -> future.completeExceptionally(exception));

    return future;
  }

  @Override
  public CompletableFuture<Boolean> unloadWorld(World world) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (world == null) {
      future.complete(true);
      return future;
    }

    LeonGunWarNeo.newChain()
        .sync(
            () -> {
              List<Player> players = world.getPlayers();

              if (!players.isEmpty()) {
                ServerTransferUtils.sendToLobby(players);
              }
            })
        .async(
            () -> {
              // 最長で10秒間、プレイヤーが居なくなるのを待つ
              int tryCount = 0;
              while (!world.getPlayers().isEmpty()) {
                try {
                  Thread.sleep(500);
                } catch (InterruptedException ex) {
                  Thread.currentThread().interrupt();
                }

                tryCount++;
                if (tryCount >= 20) {
                  return;
                }
              }
            })
        .asyncFirst(() -> Bukkit.unloadWorld(world, false))
        .asyncLast(future::complete)
        .execute((exception, task) -> future.completeExceptionally(exception));

    return future;
  }
}
