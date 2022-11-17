package net.azisaba.lgwneo.animation;

public interface Animation {

  void start();

  void cancel();

  void setCallback(Runnable callback);

}
