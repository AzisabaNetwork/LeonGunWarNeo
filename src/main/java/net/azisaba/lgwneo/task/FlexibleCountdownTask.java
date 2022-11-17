package net.azisaba.lgwneo.task;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * カウントダウンをするタスク
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class FlexibleCountdownTask {

  private final AtomicReference<BukkitTask> task = new AtomicReference<>();

  private final AtomicInteger remainingTime;
  @Setter
  private Consumer<Integer> timeElapsedAction;

  private final int initialValue;

  public FlexibleCountdownTask(int seconds, Consumer<Integer> timeElapsedAction) {
    this.remainingTime = new AtomicInteger(seconds);
    this.timeElapsedAction = timeElapsedAction;
    initialValue = seconds;
  }

  public FlexibleCountdownTask(int seconds) {
    this.remainingTime = new AtomicInteger(seconds);
    initialValue = seconds;
  }

  /**
   * カウントダウンをスタートする。すでにカウントダウンが行われている場合はそれを止めて最初からに上書きする
   */
  public void startCountdown(JavaPlugin plugin) {
    task.getAndUpdate(
        task -> {
          if (task != null) {
            task.cancel();
          }

          remainingTime.set(initialValue);
          return new BukkitRunnable() {
            @Override
            public void run() {
              int time = remainingTime.decrementAndGet();
              timeElapsedAction.accept(time);

              if (time <= 0) {
                this.cancel();
              }
            }
          }.runTaskTimer(plugin, 0L, 20L);
        });
  }

  /**
   * カウントダウンを停止する
   */
  public void stopCountdown() {
    task.getAndUpdate(
        task -> {
          if (task != null) {
            task.cancel();
          }

          return null;
        });
  }

  /**
   * 残り時間を取得する
   *
   * @return カウントダウンの残り時間
   */
  public int getRemainingTime() {
    return remainingTime.get();
  }

  /**
   * 残り時間を設定する
   *
   * @param newValue 新しい残り時間
   */
  public void setRemainingTime(int newValue) {
    remainingTime.set(newValue);
  }
}
