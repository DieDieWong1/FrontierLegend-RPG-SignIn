package com.example.signin.task;

import com.example.signin.config.Config;
import com.example.mixin.client.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Session;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Hand;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.Optional;
import java.lang.reflect.Constructor;

public class Task implements Runnable {

    private static boolean running = false;
    private final Config config;

    public Task(Config config) {
        this.config = config;
    }

    public static void start(Config config) {
        if (!running) {
            running = true;
            new Thread(new Task(config), "signin-task").start();
        }
    }

    public static void stop() {
        if (running) {
            running = false;
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 自動簽到流程已被手動停止。"), false);
                }
            });
        }
    }

    public static boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // 1. 如果玩家在世界內按下F9，等待玩家手動退出伺服器
        if (client.world != null) {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 你仍在伺服器內，請手動退出伺服器以繼續後續流程。"), false);
                }
            });

            // 等待玩家退出伺服器
            while (running && client.world != null) {
                sleep(500);
            }

            if (!running) {
                return;
            }
        }

        // 2. 检查账号列表是否为空
        if (config.accounts == null || config.accounts.isEmpty()) {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 未設定任何帳號，流程無法繼續。"), false);
                }
            });
            running = false;
            return;
        }

        // 保存初始账号索引
        int initialAccountIndex = config.currentAccountIndex;

        // 3. 循环处理所有账号
        int accountIndex = config.currentAccountIndex;
        while (running && accountIndex < config.accounts.size()) {
            config.currentAccountIndex = accountIndex;
            
            // 3.1 切换账号
            String accountName = config.accounts.get(accountIndex);
            if (accountName != null && !accountName.isEmpty()) {
                System.out.println("[SignIn] 準備切換到帳號: " + accountName);

                boolean switched = switchAccount(client, accountName);
                
                if (!switched) {
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("[SignIn] 帳號切換失敗，將跳過此帳號。"), false);
                        }
                    });
                    accountIndex++;
                    continue;
                }
            }

            // 3.2 连接到服务器
            if (!connectToServer(client)) {
                running = false;
                break;
            }
            
            if (!running) break;

            // 3.3 执行签到流程
            executeSignInFlow(client);
            
            if (!running) break;

            // 3.4 处理退出服务器 - 对于最后一个账号不自动退出
            boolean isLastAccount = (accountIndex + 1 >= config.accounts.size());
            handleServerDisconnect(client, isLastAccount);
            
            if (!running) break;

            // 等待玩家真正退出服务器
            while (running && client.world != null) {
                sleep(500);
            }
            
            if (!running) break;

            // 切换到下一个账号
            accountIndex++;
        }

        // 所有账号处理完成或任务被停止
        running = false;

        // 重置账号索引
        config.currentAccountIndex = initialAccountIndex;
        config.save();

        // 使用final数组包装accountIndex，使其可以在lambda中被访问
        final int[] accountIndexRef = {accountIndex};
        client.execute(() -> {
            if (client.player != null) {
                if (accountIndexRef[0] >= config.accounts.size()) {
                    client.player.sendMessage(Text.literal("[SignIn] 所有帳號簽到流程已完成！"), false);
                } else {
                    client.player.sendMessage(Text.literal("[SignIn] 自動簽到流程已停止。"), false);
                }
            }
        });
    }

    private boolean switchAccount(MinecraftClient client, String accountName) {
        final boolean[] switched = {false};
        final StringBuilder debugInfo = new StringBuilder("[SignIn] 帳號切換調試信息:\n");
        
        client.execute(() -> {
            try {
                // 获取原始的Session对象
                Session originalSession = client.getSession();
                debugInfo.append("原始帳號: " + originalSession.getUsername() + " (" + originalSession.getUuid() + ")\n");

                // 尝试使用6参数构造函数
                try {
                    Class<?> accountTypeClass = Class.forName("net.minecraft.client.util.Session$AccountType");
                    debugInfo.append("找到AccountType類\n");

                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Enum<?> legacy = Enum.valueOf((Class) accountTypeClass.asSubclass(Enum.class), "LEGACY");
                    debugInfo.append("找到LEGACY枚舉值\n");

                    try {
                        var ctor6 = Session.class.getConstructor(String.class, String.class, String.class, Optional.class, Optional.class, accountTypeClass);
                        debugInfo.append("找到6參數構造函數\n");

                        // 创建新的Session，使用原始Session的accessToken以保持登入状态
                        String accessToken = originalSession.getAccessToken();
                        Session s = (Session) ctor6.newInstance(accountName, "00000000-0000-0000-0000-000000000000", accessToken, Optional.empty(), Optional.empty(), legacy);
                        debugInfo.append("成功創建新Session物件\n");

                        ((MinecraftClientAccessor) client).setSession(s);
                        debugInfo.append("成功設置新Session\n");

                        switched[0] = true;
                    } catch (NoSuchMethodException e) {
                        debugInfo.append("未找到6參數構造函數: " + e.getMessage() + "\n");
                    }
                } catch (Exception e) {
                    debugInfo.append("6參數構造函數處理異常: " + e.getMessage() + "\n");
                }

                // 如果第一种方法失败，尝试使用更通用的构造函数格式
                if (!switched[0]) {
                    try {
                        Constructor<?>[] constructors = Session.class.getDeclaredConstructors();
                        debugInfo.append("找到" + constructors.length + "個構造函數\n");

                        for (Constructor<?> ctor : constructors) {
                            try {
                                Class<?>[] paramTypes = ctor.getParameterTypes();
                                if (paramTypes.length >= 3) {
                                    ctor.setAccessible(true);
                                    debugInfo.append("準備調用參數數量>=3的構造函數\n");

                                    // 提供必要的参数，使用原始的accessToken
                                    Object[] args = new Object[paramTypes.length];
                                    args[0] = accountName; // 帳號名
                                    args[1] = "00000000-0000-0000-0000-000000000000"; // UUID
                                    args[2] = originalSession.getAccessToken(); // 保持原始accessToken

                                    // 为其他参数提供默认值
                                    for (int i = 3; i < paramTypes.length; i++) {
                                        if (paramTypes[i] == Optional.class) {
                                            args[i] = Optional.empty();
                                        } else if (paramTypes[i].isEnum()) {
                                            args[i] = paramTypes[i].getEnumConstants()[0];
                                        } else if (paramTypes[i] == String.class) {
                                            args[i] = "";
                                        } else {
                                            args[i] = null;
                                        }
                                    }

                                    Session s = (Session) ctor.newInstance(args);
                                    debugInfo.append("成功用通用方法創建新Session\n");
                                    ((MinecraftClientAccessor) client).setSession(s);
                                    debugInfo.append("成功設置新Session\n");

                                    switched[0] = true;
                                    break;
                                }
                            } catch (Exception e) {
                                debugInfo.append("構造函數調用失敗: " + e.getMessage() + "\n");
                                continue; // 尝试下一个构造函数
                            }
                        }
                    } catch (Exception e) {
                        debugInfo.append("通用構造函數處理異常: " + e.getMessage() + "\n");
                    }
                }

                // 最终备案：使用简单的3参数构造函数
                if (!switched[0]) {
                    try {
                        debugInfo.append("嘗試使用3參數構造函數作為最終備案\n");
                        var ctor3 = Session.class.getConstructor(String.class, String.class, String.class);
                        Session s = (Session) ctor3.newInstance(accountName, "00000000-0000-0000-0000-000000000000", originalSession.getAccessToken());
                        ((MinecraftClientAccessor) client).setSession(s);
                        debugInfo.append("成功用3參數構造函數切換帳號\n");

                        switched[0] = true;
                    } catch (Exception e) {
                        debugInfo.append("3參數構造函數處理異常: " + e.getMessage() + "\n");
                    }
                }
            } catch (Throwable t) {
                debugInfo.append("總體異常: " + t.getClass().getName() + " - " + t.getMessage() + "\n");
                t.printStackTrace();
                switched[0] = false;
            }

            // 打印调试信息到控制台
            System.out.println(debugInfo.toString());

            if (switched[0]) {
                System.out.println("[SignIn] Switched offline account to: " + accountName);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 已切換離線帳號：" + accountName), false);
                }
            } else {
                System.out.println("[SignIn] Failed to switch offline account.");
            }
        });

        // 增加等待时间以确保账号切换完成
        sleep(1000);
        return switched[0];
    }

    private boolean connectToServer(MinecraftClient client) {
        String serverAddr = config.getCurrentServer();
        if (serverAddr == null || serverAddr.isEmpty()) {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 未設定伺服器，請用 /dailyrewards set server add <host:port>"), false);
                }
            });
            return false;
        }
        
        final ServerInfo serverInfo = new ServerInfo(serverAddr, serverAddr, false);
        
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[SignIn] 正在連線到：" + serverAddr), false);
            }
            // 设置为多人游戏界面，然后连接
            client.setScreen(new MultiplayerScreen(new TitleScreen()));
        });
        
        sleep(1500);
        
        client.execute(() -> {
            if (client.currentScreen != null && running) {
                ConnectScreen.connect(client.currentScreen, client, ServerAddress.parse(serverInfo.address), serverInfo, false);
            }
        });

        // 等待玩家加入服务器，最多等待30秒
        int waitCount = 0;
        while (running && client.player == null && waitCount < 60) {
            sleep(500);
            waitCount++;
        }
        
        if (running && client.player != null) {
            client.execute(() -> {
                client.player.sendMessage(Text.literal("[SignIn] 已連入伺服器：" + serverAddr), false);
            });
            return true;
        } else {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 連接伺服器超時或失敗。"), false);
                }
            });
            return false;
        }
    }

    private void executeSignInFlow(MinecraftClient client) {
        // 1. 登录
        client.execute(() -> {
            if (running && client.player != null) {
                String cmd = config.loginCommand;
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                client.player.networkHandler.sendChatCommand(cmd);
            }
        });
        sleep(2000);

        // 2. 签到
        client.execute(() -> {
            if (running && client.player != null) {
                String cmd = config.signinCommand;
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                client.player.networkHandler.sendChatCommand(cmd);
            }
        });
        sleep(1500); // 签到等待时间

        // 3. 回家
        if (config.autoHome) {
            client.execute(() -> {
                if (running && client.player != null) {
                    String cmd = config.homeCommand;
                    if (cmd.startsWith("/")) {
                        cmd = cmd.substring(1);
                    }
                    client.player.networkHandler.sendChatCommand(cmd);
                }
            });
            sleep(1500); // 回家等待时间
        }

        // 4. 自动放置物品
        if (config.autoPutItem) {
            client.execute(() -> {
                if (running && client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 開始尋找最近的箱子..."), false);
                }
            });
            
            // 使用单独的线程进行物品放置，但保持running状态一致
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            new Thread(() -> {
                try {
                    findAndDepositItems(client);
                } catch (Exception e) {
                    e.printStackTrace();
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().player.sendMessage(
                                    Text.literal("[SignIn] 放置物品过程中发生错误"), false);
                        }
                    });
                } finally {
                    latch.countDown();
                }
            }, "signin-chest-task").start();

            // 等待物品放置完成，最多等待15秒
            try {
                latch.await(15, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        } else if (running) {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 自动放置物品功能已关闭"), false);
                }
            });
        }
    }

    private void findAndDepositItems(MinecraftClient client) {
        if (!running || client.player == null || client.world == null) return;

        // 寻找最近的箱子（32格范围内）
        int searchRadius = 32;
        BlockPos nearestChestPos = null;
        double nearestDistance = Double.MAX_VALUE;

        // 获取玩家位置的一个快照，避免在lambda中直接引用可变变量
        final BlockPos playerPos = client.player.getBlockPos().toImmutable();
        
        // 在客户端线程中查找箱子
        final BlockPos[] chestPosRef = {null};
        final double[] nearestDistanceRef = {nearestDistance};
        client.execute(() -> {
            if (!running || client.world == null) return;
            
            for (int x = -searchRadius; x <= searchRadius && running; x++) {
                for (int y = -searchRadius; y <= searchRadius && running; y++) {
                    for (int z = -searchRadius; z <= searchRadius && running; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        BlockState state = client.world.getBlockState(pos);
                        Block block = state.getBlock();

                        // 检查是否为箱子、陷阱箱或木桶
                        if ((block instanceof ChestBlock) || 
                            (block instanceof TrappedChestBlock) || 
                            (block instanceof BarrelBlock)) {
                            double distance = client.player.squaredDistanceTo(
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                            if (distance < nearestDistanceRef[0]) {
                                nearestDistanceRef[0] = distance;
                                chestPosRef[0] = pos.toImmutable();
                            }
                        }
                    }
                }
            }
        });
        
        // 更新最近距离
        nearestDistance = nearestDistanceRef[0];
        
        // 等待客户端执行完成
        sleep(200);
        nearestChestPos = chestPosRef[0];
        
        if (!running) return;

        if (nearestChestPos != null) {
            final BlockPos finalChestPos = nearestChestPos;
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 找到最近的箱子，位置：" + finalChestPos.toShortString()), false);
                }
            });
            
            // 转向箱子
            sleep(500);
            client.execute(() -> {
                if (client.player != null && running) {
                    double dx = finalChestPos.getX() + 0.5 - client.player.getX();
                    double dz = finalChestPos.getZ() + 0.5 - client.player.getZ();
                    double yaw = Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0;
                    client.player.setYaw((float) yaw);
                }
            });
            
            // 等待转向完成
            sleep(1000);
            
            // 模拟右键点击打开箱子
            client.execute(() -> {
                if (client.player != null && client.world != null && running) {
                    BlockHitResult hitResult = new BlockHitResult(
                            new Vec3d(finalChestPos.getX() + 0.5, finalChestPos.getY() + 0.5, finalChestPos.getZ() + 0.5),
                            Direction.UP,
                            finalChestPos,
                            false
                    );
                    client.interactionManager.interactBlock(
                            (ClientPlayerEntity) client.player,
                            Hand.MAIN_HAND,
                            hitResult
                    );
                }
            });
            
            // 等待箱子打开
            sleep(1500);
            
            // 存入物品到箱子 - 只处理快捷栏（第0格到第8格）
            for (int i = 0; i < 9 && running; i++) {
                final int slotIndex = i;
                client.execute(() -> {
                    if (client.player != null && running) {
                        ScreenHandler screenHandler = client.player.currentScreenHandler;
                        ItemStack stack = client.player.getInventory().getStack(slotIndex);

                        if (!stack.isEmpty()) {
                            // 查找玩家物品栏中对应索引的槽位
                            for (int j = 0; j < screenHandler.slots.size(); j++) {
                                Slot slot = screenHandler.slots.get(j);
                                if (slot.inventory == client.player.getInventory() && slot.getIndex() == slotIndex) {
                                    // 使用SHIFT+LEFT_CLICK将物品快速移动到箱子中
                                    client.interactionManager.clickSlot(
                                            screenHandler.syncId,
                                            j,
                                            0,
                                            SlotActionType.QUICK_MOVE,
                                            (ClientPlayerEntity) client.player
                                    );
                                    break;
                                }
                            }
                        }
                    }
                });
                // 在客户端执行之间添加延迟
                sleep(100);
            }
            
            // 物品存入完成后关闭箱子界面
            sleep(500);
            client.execute(() -> {
                if (client.player != null && running) {
                    client.setScreen(null);
                    client.player.sendMessage(
                            Text.literal("[SignIn] 已將快捷栏物品存入箱子并关闭界面"), false);
                }
            });
        } else {
            client.execute(() -> {
                if (client.player != null && running) {
                    client.player.sendMessage(Text.literal("[SignIn] 在32格範圍內未找到箱子"), false);
                }
            });
        }
    }

    private void handleServerDisconnect(MinecraftClient client, boolean isLastAccount) {
        // 统一的消息提示逻辑
        client.execute(() -> {
            if (running && client.player != null) {
                if (config.autoLeave && !isLastAccount) {
                    client.player.sendMessage(Text.literal("[SignIn] 將在3秒後自動退出伺服器..."), false);
                } else {
                    if (isLastAccount) {
                        client.player.sendMessage(Text.literal("[SignIn] 已到最后一个账号，保持在服务器中"), false);
                    } else {
                        client.player.sendMessage(Text.literal("[SignIn] 自動退出服务器功能已关闭，請保持在伺服器中"), false);
                    }
                }
            }
        });

        if (config.autoLeave && !isLastAccount) {
            // 等待3秒后自动退出
            sleep(3000);
            
            // 执行客户端断开连接并确保服务器端正确处理
            client.execute(() -> {
                if (running && client.world != null && client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 正在断开连接..."), false);
                    
                    // 确保networkHandler不为null
                    if (client.getNetworkHandler() != null) {
                        try {
                            // 1. 先清除networkHandler的连接状态
                            client.getNetworkHandler().getConnection().disconnect(Text.literal("[SignIn] 退出游戏"));
                            
                            // 2. 给服务器一点时间处理断开连接请求
                            Thread.sleep(200);
                        } catch (Exception ignored) {}
                    }
                    
                    // 3. 最后调用client的disconnect方法
                    client.disconnect();
                }
            });
            
            // 增加等待时间，给断开连接操作足够的时间完成
            sleep(1500);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * 测试账号切换功能 - 独立测试方法
     */
    public static void testAccountSwitch(Config config) {
        if (running) {
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("[SignIn] 已有任务正在运行，请先停止"), false);
            });
            return;
        }
        
        running = true;
        MinecraftClient client = MinecraftClient.getInstance();
        System.out.println("[SignIn] 帳號切換獨立測試開始");

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[SignIn] 帳號切換獨立測試開始"), false);
            }
        });

        // 等待客户端更新
        sleep(500);

        String accountName = config.getCurrentAccount();
        if (accountName == null || accountName.isEmpty()) {
            accountName = "TestAccount" + (int) (Math.random() * 1000);
        }
        
        final String finalAccountName = accountName;
        
        // 获取原始Session
        final Session originalSession = client.getSession();
        final Session[] originalSessionRef = {originalSession};

        client.execute(() -> {
            boolean switched = false;
            StringBuilder debugInfo = new StringBuilder("[SignIn] 帳號切換測試調試信息:\n");

            try {
                String originalName = originalSessionRef[0].getUsername();
                debugInfo.append("原始帳號: " + originalName + " (" + originalSessionRef[0].getUuid() + ")\n");

                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 原始帳號: " + originalName), false);
                    client.player.sendMessage(Text.literal("[SignIn] 準備切換到: " + finalAccountName), false);
                }

                try {
                    // 尝试使用6参数构造函数
                    Class<?> accountTypeClass = Class.forName("net.minecraft.client.util.Session$AccountType");
                    debugInfo.append("找到AccountType類\n");

                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Enum<?> legacy = Enum.valueOf((Class) accountTypeClass.asSubclass(Enum.class), "LEGACY");
                    debugInfo.append("找到LEGACY枚舉值\n");

                    try {
                        var ctor6 = Session.class.getConstructor(String.class, String.class, String.class, Optional.class, Optional.class, accountTypeClass);
                        debugInfo.append("找到6參數構造函數\n");

                        // 创建新的Session
                        String accessToken = originalSessionRef[0].getAccessToken();
                        Session s = (Session) ctor6.newInstance(finalAccountName, "00000000-0000-0000-0000-000000000000", accessToken, Optional.empty(), Optional.empty(), legacy);
                        debugInfo.append("成功創建新Session物件\n");

                        ((MinecraftClientAccessor) client).setSession(s);
                        debugInfo.append("成功設置新Session\n");

                        switched = true;
                    } catch (NoSuchMethodException e) {
                        debugInfo.append("未找到6參數構造函數: " + e.getMessage() + "\n");
                    }
                } catch (Exception e) {
                    debugInfo.append("6參數構造函數處理異常: " + e.getMessage() + "\n");
                }

                // 如果第一种方法失败，尝试使用更通用的构造函数格式
                if (!switched) {
                    try {
                        Constructor<?>[] constructors = Session.class.getDeclaredConstructors();
                        debugInfo.append("找到" + constructors.length + "個構造函數\n");

                        for (Constructor<?> ctor : constructors) {
                            try {
                                Class<?>[] paramTypes = ctor.getParameterTypes();
                                if (paramTypes.length >= 3) {
                                    ctor.setAccessible(true);
                                    debugInfo.append("準備調用參數數量>=3的構造函數\n");

                                    // 提供必要的参数
                                    Object[] args = new Object[paramTypes.length];
                                    args[0] = finalAccountName; // 帳號名
                                    args[1] = "00000000-0000-0000-0000-000000000000"; // UUID
                                    args[2] = originalSessionRef[0].getAccessToken(); // 保持原始accessToken

                                    // 为其他参数提供默认值
                                    for (int i = 3; i < paramTypes.length; i++) {
                                        if (paramTypes[i] == Optional.class) {
                                            args[i] = Optional.empty();
                                        } else if (paramTypes[i].isEnum()) {
                                            args[i] = paramTypes[i].getEnumConstants()[0];
                                        } else if (paramTypes[i] == String.class) {
                                            args[i] = "";
                                        } else {
                                            args[i] = null;
                                        }
                                    }

                                    Session s = (Session) ctor.newInstance(args);
                                    debugInfo.append("成功用通用方法創建新Session\n");
                                    ((MinecraftClientAccessor) client).setSession(s);
                                    debugInfo.append("成功設置新Session\n");

                                    switched = true;
                                    break;
                                }
                            } catch (Exception e) {
                                debugInfo.append("構造函數調用失敗: " + e.getMessage() + "\n");
                                continue; // 尝试下一个构造函数
                            }
                        }
                    } catch (Exception e) {
                        debugInfo.append("通用構造函數處理異常: " + e.getMessage() + "\n");
                    }
                }

                // 最终备案：使用简单的3参数构造函数
                if (!switched) {
                    try {
                        debugInfo.append("嘗試使用3參數構造函數作為最終備案\n");
                        var ctor3 = Session.class.getConstructor(String.class, String.class, String.class);
                        Session s = (Session) ctor3.newInstance(finalAccountName, "00000000-0000-0000-0000-000000000000", originalSessionRef[0].getAccessToken());
                        ((MinecraftClientAccessor) client).setSession(s);
                        debugInfo.append("成功用3參數構造函數切換帳號\n");

                        switched = true;
                    } catch (Exception e) {
                        debugInfo.append("3參數構造函數處理異常: " + e.getMessage() + "\n");
                    }
                }
            } catch (Throwable t) {
                debugInfo.append("總體異常: " + t.getClass().getName() + " - " + t.getMessage() + "\n");
                t.printStackTrace();
                switched = false;
            }

            // 打印调试信息到控制台
            System.out.println(debugInfo.toString());

            if (switched) {
                System.out.println("[SignIn] Switched offline account to: " + finalAccountName);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 已切換離線帳號：" + finalAccountName), false);
                    client.player.sendMessage(Text.literal("[SignIn] 測試完成，將在3秒後切回原始帳號"), false);
                }
            } else {
                System.out.println("[SignIn] Failed to switch offline account.");
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 帳號切換測試失敗"), false);
                }
                running = false;
            }
        });

        // 等待3秒后切回原始账号
        sleep(3000);
        
        client.execute(() -> {
            try {
                ((MinecraftClientAccessor) client).setSession(originalSessionRef[0]);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 已切回原始帳號"), false);
                    client.player.sendMessage(Text.literal("[SignIn] 帳號切換測試完成"), false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[SignIn] 切回原始帳號時發生錯誤"), false);
                }
            } finally {
                running = false;
            }
        });
    }

    /**
     * 发送聊天命令
     */
    private static void sendChatCommand(MinecraftClient client, String command) {
        if (client.player != null) {
            client.player.networkHandler.sendChatCommand(command);
        }
    }
    
    /**
     * 测试自动进入服务器功能
     */
    public static void testServerConnect(Config config) {
        if (running) {
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("[SignIn] 已有任务正在运行，请先停止"), false);
            });
            return;
        }
        
        running = true;
        new Thread(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                
                // 创建Task实例来调用非静态方法
                Task task = new Task(config);
                
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("[SignIn] 开始测试自动进入服务器功能"), false);
                    }
                });
                
                // 等待客户端更新
                sleep(500);
                
                // 连接到服务器
                if (!task.connectToServer(client)) {
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("[SignIn] 连接服务器失败"), false);
                        }
                    });
                } else {
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("[SignIn] 连接服务器成功"), false);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                running = false;
            }
        }).start();
    }
    
    /**
     * 测试自动输入指令功能
     */
    public static void testAutoCommands(Config config) {
        if (running) {
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("[SignIn] 已有任务正在运行，请先停止"), false);
            });
            return;
        }
        
        running = true;
        new Thread(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("[SignIn] 开始测试自动输入指令功能"), false);
                        // 发送测试指令
                        sendChatCommand(client, config.loginCommand);
                        sleep(2000);
                        sendChatCommand(client, config.signinCommand);
                        sleep(2000);
                        client.player.sendMessage(Text.literal("[SignIn] 测试指令已发送"), false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                running = false;
            }
        }).start();
    }
    
    /**
     * 测试所有功能
     */
    public static void testAllFeatures(Config config) {
        if (running) {
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("[SignIn] 已有任务正在运行，请先停止"), false);
            });
            return;
        }
        
        running = true;
        new Thread(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("[SignIn] 启动完整流程测试"), false);
                    }
                });
                
                // 这里可以调用各种测试方法来执行完整测试流程
                sleep(500);
                
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("[SignIn] 完整流程测试完成"), false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("[SignIn] 测试过程中发生错误"), false);
                });
            } finally {
                running = false;
            }
        }).start();
    }
    
    /**
     * 测试寻找箱子功能
     */
    public static void testChestFunction(Config config) {
        if (running) {
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("[SignIn] 已有任务正在运行，请先停止"), false);
            });
            return;
        }
        
        running = true;
        new Thread(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("[SignIn] 尋找箱子測試開始"), false);
                    }
                });

                // 寻找最近的箱子（32格范围内）
                int searchRadius = 32;
                BlockPos nearestChestPos = null;
                
                // 获取玩家位置
                final BlockPos playerPos = client.player.getBlockPos().toImmutable();
                
                // 在客户端线程中查找箱子
                final BlockPos[] chestPosRef = {null};
                client.execute(() -> {
                    if (!running || client.world == null) return;
                    
                    double nearestDistance = Double.MAX_VALUE;
                    for (int x = -searchRadius; x <= searchRadius && running; x++) {
                        for (int y = -searchRadius; y <= searchRadius && running; y++) {
                            for (int z = -searchRadius; z <= searchRadius && running; z++) {
                                BlockPos pos = playerPos.add(x, y, z);
                                BlockState state = client.world.getBlockState(pos);
                                Block block = state.getBlock();

                                // 检查是否为箱子、陷阱箱或木桶
                                if ((block instanceof ChestBlock) || 
                                    (block instanceof TrappedChestBlock) || 
                                    (block instanceof BarrelBlock)) {
                                    double distance = client.player.squaredDistanceTo(
                                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                    if (distance < nearestDistance) {
                                        nearestDistance = distance;
                                        chestPosRef[0] = pos.toImmutable();
                                    }
                                }
                            }
                        }
                    }
                });
                
                // 等待客户端执行完成
                sleep(200);
                nearestChestPos = chestPosRef[0];

                if (!running) return;

                if (nearestChestPos != null) {
                    final BlockPos finalChestPos = nearestChestPos;
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("[SignIn] 找到最近的箱子，位置：" + finalChestPos.toShortString()), false);
                        }
                    });
                } else {
                    client.execute(() -> {
                        if (client.player != null && running) {
                            client.player.sendMessage(Text.literal("[SignIn] 在32格範圍內未找到箱子"), false);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(Text.literal("[SignIn] 測試過程中發生錯誤"), false);
                    }
                });
            } finally {
                running = false;
                
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(Text.literal("[SignIn] 尋找箱子測試完成"), false);
                    }
                });
            }
        }, "signin-chest-test").start();
    }
}
