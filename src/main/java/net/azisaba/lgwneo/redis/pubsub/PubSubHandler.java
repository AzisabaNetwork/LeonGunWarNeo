package net.azisaba.lgwneo.redis.pubsub;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

@RequiredArgsConstructor
public class PubSubHandler {

  private final JedisPool jedisPool;

  private final List<ExecutorService> executors = new ArrayList<>();

  public void startSubscribe(JedisPubSub subscriber, String... channels) {
    ExecutorService executor = Executors.newFixedThreadPool(1);

    Runnable task =
        () -> {
          try (Jedis jedis = jedisPool.getResource()) {
            jedis.subscribe(subscriber, channels);
          }
        };

    executor.submit(getDelayRunnable(executor, task));
    executors.add(executor);
  }

  public void startPatternSubscribe(JedisPubSub subscriber, String... patterns) {
    ExecutorService executor = Executors.newFixedThreadPool(1);

    Runnable task =
        () -> {
          try (Jedis jedis = jedisPool.getResource()) {
            jedis.psubscribe(subscriber, patterns);
          }
        };

    executor.submit(getDelayRunnable(executor, task));
    executors.add(executor);
  }

  public void unsubscribeAll() {
    executors.forEach(ExecutorService::shutdownNow);
    executors.clear();
  }

  private Runnable getDelayRunnable(ExecutorService executor, Runnable runnable) {
    return () -> {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      try {
        runnable.run();
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        executor.submit(getDelayRunnable(executor, runnable));
      }
    };
  }
}
