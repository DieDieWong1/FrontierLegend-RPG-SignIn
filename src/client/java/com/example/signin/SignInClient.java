package com.example.signin;

import com.example.signin.config.Config;
import com.example.signin.task.Task;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

public class SignInClient implements ClientModInitializer {

    private static Config config;

    @Override
    public void onInitializeClient() {
        config = Config.load();

        com.example.signin.keybinding.KeyBindings.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (com.example.signin.keybinding.KeyBindings.consumeStartPressed()) {
                Task.start(config);
            }

            while (com.example.signin.keybinding.KeyBindings.consumeStopPressed()) {
                Task.stop();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("dailyrewards")
                            .then(ClientCommandManager.literal("help")
                                    .executes(ctx -> {
                                        net.minecraft.text.MutableText helpText = net.minecraft.text.Text.literal("").append(
                                                net.minecraft.text.Text.literal("[SignIn] 自動簽到模組使用說明\n\n").formatted(net.minecraft.util.Formatting.YELLOW)
                                        ).append(
                                                net.minecraft.text.Text.literal("基本命令:\n").formatted(net.minecraft.util.Formatting.GOLD)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set login <指令> - 設定登入指令\n").formatted(net.minecraft.util.Formatting.AQUA)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set sign <指令> - 設定簽到指令\n").formatted(net.minecraft.util.Formatting.AQUA)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set home <指令> - 設定回家/地皮指令\n").formatted(net.minecraft.util.Formatting.AQUA)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set hand <槽位> - 設定開箱時使用的物品欄位 (0-8)\n").formatted(net.minecraft.util.Formatting.AQUA)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set account add <前綴> <起始數字> <數量> - 批量添加帳號\n").formatted(net.minecraft.util.Formatting.GREEN)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set account add <帳號名> - 添加單個帳號\n").formatted(net.minecraft.util.Formatting.GREEN)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set account use <索引> - 切換使用的帳號索引\n").formatted(net.minecraft.util.Formatting.GREEN)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set account clear - 清空帳號列表\n").formatted(net.minecraft.util.Formatting.GREEN)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set server add <地址> - 添加伺服器地址\n").formatted(net.minecraft.util.Formatting.BLUE)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set server use <索引> - 切換使用的伺服器索引\n").formatted(net.minecraft.util.Formatting.BLUE)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards set server clear - 清空伺服器列表\n").formatted(net.minecraft.util.Formatting.BLUE)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards auto leave on/off - 開啟/關閉自動退出伺服器功能\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards auto putitem on/off - 開啟/關閉自動放置物品功能\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                        ).append(
                                                  net.minecraft.text.Text.literal("/dailyrewards auto home on/off - 開啟/關閉自動回家/地皮指令功能\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                          ).append(
                                                  net.minecraft.text.Text.literal("/dailyrewards auto server on/off - 開啟/關閉自動進入伺服器功能\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                          ).append(
                                                   net.minecraft.text.Text.literal("/dailyrewards auto changeip on/off - 開啟/關閉自動轉IP功能\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                           ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards test account - 測試帳號轉換功能\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards test server - 測試自動進入伺服器功能\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards test cmds - 測試自動輸入指令功能\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards test chest - 測試尋找箱子功能\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards test all - 測試所有功能\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                        ).append(
                                                net.minecraft.text.Text.literal("/dailyrewards info - 顯示當前設定\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                        ).append(
                                                net.minecraft.text.Text.literal("\n快捷鍵:\n").formatted(net.minecraft.util.Formatting.GOLD)
                                        ).append(
                                                net.minecraft.text.Text.literal("F9 - 開始自動簽到流程\n").formatted(net.minecraft.util.Formatting.RED)
                                        ).append(
                                                net.minecraft.text.Text.literal("F12 - 停止自動簽到流程\n").formatted(net.minecraft.util.Formatting.RED)
                                        );
                                        ctx.getSource().sendFeedback(helpText);
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("set")
                                    .then(ClientCommandManager.literal("login")
                                            .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                                    .executes(context -> {
                                                        config.loginCommand = StringArgumentType.getString(context, "command");
                                                        config.save();
                                                        context.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 設定登入指令成功：" + config.loginCommand).formatted(net.minecraft.util.Formatting.GREEN));
                                                        return 1;
                                                    })))
                                    .then(ClientCommandManager.literal("sign")
                                            .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                                    .executes(context -> {
                                                        config.signinCommand = StringArgumentType.getString(context, "command");
                                                        config.save();
                                                        context.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 設定簽到指令成功：" + config.signinCommand).formatted(net.minecraft.util.Formatting.GREEN));
                                                        return 1;
                                                    })))
                                    .then(ClientCommandManager.literal("home")
                                            .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                                    .executes(context -> {
                                                        config.homeCommand = StringArgumentType.getString(context, "command");
                                                        config.save();
                                                        context.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 設定地皮指令成功：" + config.homeCommand).formatted(net.minecraft.util.Formatting.GREEN));
                                                        return 1;
                                                    })))
                                    .then(ClientCommandManager.literal("hand")
                                            .then(ClientCommandManager.argument("slot", IntegerArgumentType.integer(0, 8))
                                                    .executes(ctx -> {
                                                        config.handSlot = IntegerArgumentType.getInteger(ctx, "slot");
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已設定開箱物品欄位：" + config.handSlot).formatted(net.minecraft.util.Formatting.GREEN));
                                                        return 1;
                                                    })))
                                    .then(ClientCommandManager.literal("account")
                                            .then(ClientCommandManager.literal("add")
                                                    .then(ClientCommandManager.argument("prefix", StringArgumentType.word())
                                                            .then(ClientCommandManager.argument("startNum", IntegerArgumentType.integer(0))
                                                                    .then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1))
                                                                            .executes(ctx -> {
                                                                                String prefix = StringArgumentType.getString(ctx, "prefix");
                                                                                int startNum = IntegerArgumentType.getInteger(ctx, "startNum");
                                                                                int count = IntegerArgumentType.getInteger(ctx, "count");

                                                                                int added = 0;
                                                                                for (int i = 0; i < count; i++) {
                                                                                    String accountName = prefix + (startNum + i);
                                                                                    if (!config.accounts.contains(accountName)) {
                                                                                        config.accounts.add(accountName);
                                                                                        added++;
                                                                                    }
                                                                                }

                                                                                if (added > 0) {
                                                                                    config.save();
                                                                                }

                                                                                ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 新增帳號：" + added + " 個（" + prefix + startNum + " ~ " + prefix + (startNum + count - 1) + "），目前帳號列表數量：" + config.accounts.size()).formatted(net.minecraft.util.Formatting.GREEN));
                                                                                return 1;
                                                                            }))))
                                                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                                            .executes(ctx -> {
                                                                String name = StringArgumentType.getString(ctx, "name");
                                                                if (!config.accounts.contains(name)) {
                                                                    config.accounts.add(name);
                                                                    config.save();
                                                                }
                                                                ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 新增單個帳號：" + name + "，目前帳號列表：" + config.accounts).formatted(net.minecraft.util.Formatting.GREEN));
                                                                return 1;
                                                            })))
                                            .then(ClientCommandManager.literal("use")
                                                    .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(0))
                                                            .executes(ctx -> {
                                                                int idx = IntegerArgumentType.getInteger(ctx, "index");
                                                                if (config.accounts != null && !config.accounts.isEmpty()) {
                                                                    config.currentAccountIndex = Math.max(0, Math.min(idx, config.accounts.size() - 1));
                                                                    config.save();
                                                                }
                                                                ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已切換目前帳號索引：" + config.currentAccountIndex + "（帳號：" + config.getCurrentAccount() + ")").formatted(net.minecraft.util.Formatting.GREEN));
                                                                return 1;
                                                            })))
                                            .then(ClientCommandManager.literal("clear")
                                                    .executes(ctx -> {
                                                        config.accounts.clear();
                                                        config.currentAccountIndex = 0;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已清空帳號列表").formatted(net.minecraft.util.Formatting.RED));
                                                        return 1;
                                                    }))
                                    )
                                    .then(ClientCommandManager.literal("server")
                                            .then(ClientCommandManager.literal("add")
                                                    .then(ClientCommandManager.argument("address", StringArgumentType.greedyString())
                                                            .executes(ctx -> {
                                                                String raw = StringArgumentType.getString(ctx, "address");
                                                                String addr = raw
                                                                        .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                                                                        .trim();
                                                                if (addr.isEmpty()) {
                                                                    ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 伺服器位址為空，請輸入 host 或 host:port").formatted(net.minecraft.util.Formatting.RED));
                                                                    return 1;
                                                                }
                                                                if (!config.servers.contains(addr)) {
                                                                    config.servers.add(addr);
                                                                    config.save();
                                                                }
                                                                ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 新增伺服器：" + addr + "，目前伺服器列表：" + config.servers).formatted(net.minecraft.util.Formatting.BLUE));
                                                                return 1;
                                                            })))
                                            .then(ClientCommandManager.literal("use")
                                                    .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(0))
                                                            .executes(ctx -> {
                                                                int idx = IntegerArgumentType.getInteger(ctx, "index");
                                                                if (config.servers != null && !config.servers.isEmpty()) {
                                                                    config.currentServerIndex = Math.max(0, Math.min(idx, config.servers.size() - 1));
                                                                    config.save();
                                                                }
                                                                ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已切換目前伺服器索引：" + config.currentServerIndex + "（伺服器：" + config.getCurrentServer() + ")").formatted(net.minecraft.util.Formatting.BLUE));
                                                                return 1;
                                                            })))
                                            .then(ClientCommandManager.literal("clear")
                                                    .executes(ctx -> {
                                                        config.servers.clear();
                                                        config.currentServerIndex = 0;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已清空伺服器列表").formatted(net.minecraft.util.Formatting.RED));
                                                        return 1;
                                                    }))
                                    )
                            )
                            .then(ClientCommandManager.literal("test")
                                    // 測試帳號轉換功能
                                    .then(ClientCommandManager.literal("account")
                                            .executes(ctx -> {
                                                if (com.example.signin.task.Task.isRunning()) {
                                                    ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已有任務正在運行，請先停止").formatted(net.minecraft.util.Formatting.RED));
                                                    return 1;
                                                }
                                                ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 開始測試帳號轉換功能 (F12 可停止)").formatted(net.minecraft.util.Formatting.YELLOW));
                                                com.example.signin.task.Task.testAccountSwitch(config);
                                                return 1;
                                            })
                                    )
                                    // 測試自動進入伺服器功能
                                    .then(ClientCommandManager.literal("server")
                                            .executes(ctx -> {
                                                if (com.example.signin.task.Task.isRunning()) {
                                                    ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已有任務正在運行，請先停止").formatted(net.minecraft.util.Formatting.RED));
                                                    return 1;
                                                }
                                                ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 開始測試自動進入伺服器功能 (F12 可停止)").formatted(net.minecraft.util.Formatting.YELLOW));
                                                com.example.signin.task.Task.testServerConnect(config);
                                                return 1;
                                            })
                                    )
                                    // 測試自動輸入指令功能
                                    .then(ClientCommandManager.literal("cmds")
                                            .executes(ctx -> {
                                                if (com.example.signin.task.Task.isRunning()) {
                                                    ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已有任務正在運行，請先停止").formatted(net.minecraft.util.Formatting.RED));
                                                    return 1;
                                                }
                                                ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 開始測試自動輸入指令功能 (F12 可停止)").formatted(net.minecraft.util.Formatting.YELLOW));
                                                com.example.signin.task.Task.testAutoCommands(config);
                                                return 1;
                                            })
                                    )
                                    // 測試尋找箱子功能
                                    .then(ClientCommandManager.literal("chest")
                                            .executes(ctx -> {
                                                if (com.example.signin.task.Task.isRunning()) {
                                                    ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已有任務正在運行，請先停止").formatted(net.minecraft.util.Formatting.RED));
                                                    return 1;
                                                }
                                                ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 開始測試尋找最近的箱子並存入物品 (F12 可停止)").formatted(net.minecraft.util.Formatting.YELLOW));
                                                com.example.signin.task.Task.testChestFunction(config);
                                                return 1;
                                            })
                                    )
                                    // 一次性測試所有功能
                                    .then(ClientCommandManager.literal("all")
                                            .executes(ctx -> {
                                                if (com.example.signin.task.Task.isRunning()) {
                                                    ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已有任務正在運行，請先停止").formatted(net.minecraft.util.Formatting.RED));
                                                    return 1;
                                                }
                                                ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 啟動完整流程測試 (F12 可停止)").formatted(net.minecraft.util.Formatting.YELLOW));
                                                com.example.signin.task.Task.testAllFeatures(config);
                                                return 1;
                                            })
                                    )
                                    .then(ClientCommandManager.literal("show")
                                            .executes(ctx -> {
                                                net.minecraft.text.MutableText msgText = net.minecraft.text.Text.literal("").append(
                                                        net.minecraft.text.Text.literal("[SignIn] 當前設定\n").formatted(net.minecraft.util.Formatting.YELLOW)
                                                ).append(
                                                        net.minecraft.text.Text.literal("- 伺服器: " + config.getCurrentServer() + " (index=" + config.currentServerIndex + ")\n").formatted(net.minecraft.util.Formatting.BLUE)
                                                ).append(
                                                        net.minecraft.text.Text.literal("- 帳號: " + config.getCurrentAccount() + " (index=" + config.currentAccountIndex + ")\n").formatted(net.minecraft.util.Formatting.GREEN)
                                                ).append(
                                                        net.minecraft.text.Text.literal("- login: " + config.loginCommand + "\n").formatted(net.minecraft.util.Formatting.AQUA)
                                                ).append(
                                                        net.minecraft.text.Text.literal("- sign: " + config.signinCommand + "\n").formatted(net.minecraft.util.Formatting.AQUA)
                                                ).append(
                                                        net.minecraft.text.Text.literal("- home: " + config.homeCommand + "\n").formatted(net.minecraft.util.Formatting.AQUA)
                                                ).append(
                                                        net.minecraft.text.Text.literal("- 開箱物品欄位: " + config.handSlot + "\n").formatted(net.minecraft.util.Formatting.AQUA)
                                                ).append(
                                                        net.minecraft.text.Text.literal("- 自動退出伺服器: " + (config.autoLeave ? "開啟" : "關閉") + "\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                                ).append(
                                                        net.minecraft.text.Text.literal("- 自動放置物品: " + (config.autoPutItem ? "開啟" : "關閉") + "\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                                ).append(
                                                        net.minecraft.text.Text.literal("- 自動進入伺服器: " + (config.autoServer ? "開啟" : "關閉") + "\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                                ).append(
                                                        net.minecraft.text.Text.literal("- 自動轉IP: " + (config.autoChangeIp ? "開啟" : "關閉")).formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                                                );
                                                ctx.getSource().sendFeedback(msgText);
                                                return 1;
                                            })
                                    )
                            )

                            .then(ClientCommandManager.literal("auto")
                                    .then(ClientCommandManager.literal("leave")
                                            .then(ClientCommandManager.literal("on")
                                                    .executes(ctx -> {
                                                        config.autoLeave = true;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已開啟自動退出伺服器功能").formatted(net.minecraft.util.Formatting.GREEN));
                                                        return 1;
                                                    })
                                            )
                                            .then(ClientCommandManager.literal("off")
                                                    .executes(ctx -> {
                                                        config.autoLeave = false;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已關閉自動退出伺服器功能").formatted(net.minecraft.util.Formatting.RED));
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(ClientCommandManager.literal("putitem")
                                            .then(ClientCommandManager.literal("on")
                                                    .executes(ctx -> {
                                                        config.autoPutItem = true;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已開啟自動放置物品功能").formatted(net.minecraft.util.Formatting.GREEN));
                                                        return 1;
                                                    })
                                            )
                                            .then(ClientCommandManager.literal("off")
                                                    .executes(ctx -> {
                                                        config.autoPutItem = false;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已關閉自動放置物品功能").formatted(net.minecraft.util.Formatting.RED));
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(ClientCommandManager.literal("home")
                                            .then(ClientCommandManager.literal("on")
                                                    .executes(ctx -> {
                                                        config.autoHome = true;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已開啟自動回家/地皮指令功能").formatted(net.minecraft.util.Formatting.GREEN));
                                                        return 1;
                                                    })
                                            )
                                            .then(ClientCommandManager.literal("off")
                                                    .executes(ctx -> {
                                                        config.autoHome = false;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已關閉自動回家/地皮指令功能").formatted(net.minecraft.util.Formatting.RED));
                                                        return 1;
                                                    }))
                                    )
                                    .then(ClientCommandManager.literal("server")
                                            .then(ClientCommandManager.literal("on")
                                                    .executes(ctx -> {
                                                        config.autoServer = true;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已開啟自動進入伺服器功能").formatted(net.minecraft.util.Formatting.GREEN));
                                                        return 1;
                                                    })
                                            )
                                            .then(ClientCommandManager.literal("off")
                                                    .executes(ctx -> {
                                                        config.autoServer = false;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已關閉自動進入伺服器功能").formatted(net.minecraft.util.Formatting.RED));
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(ClientCommandManager.literal("changeip")
                                            .then(ClientCommandManager.literal("on")
                                                    .executes(ctx -> {
                                                        config.autoChangeIp = true;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已開啟自動轉IP功能").formatted(net.minecraft.util.Formatting.GREEN));
                                                        return 1;
                                                    })
                                            )
                                            .then(ClientCommandManager.literal("off")
                                                    .executes(ctx -> {
                                                        config.autoChangeIp = false;
                                                        config.save();
                                                        ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("[SignIn] 已關閉自動轉IP功能").formatted(net.minecraft.util.Formatting.RED));
                                                        return 1;
                                                    })
                                            )
                                    )
                            .then(ClientCommandManager.literal("info"))
                                    .executes(ctx -> {
                                        net.minecraft.text.MutableText msgText = net.minecraft.text.Text.literal("");
                                        msgText = msgText.append(net.minecraft.text.Text.literal("[SignIn] 當前設定\n").formatted(net.minecraft.util.Formatting.YELLOW));
                                        msgText = msgText.append(net.minecraft.text.Text.literal("- 伺服器: " + config.getCurrentServer() + " (index=" + config.currentServerIndex + ")\n").formatted(net.minecraft.util.Formatting.BLUE));
                                        msgText = msgText.append(net.minecraft.text.Text.literal("- 帳號: " + config.getCurrentAccount() + " (index=" + config.currentAccountIndex + ")\n").formatted(net.minecraft.util.Formatting.GREEN));
                                        msgText = msgText.append(net.minecraft.text.Text.literal("- login: " + config.loginCommand + "\n").formatted(net.minecraft.util.Formatting.AQUA));
                                        msgText = msgText.append(net.minecraft.text.Text.literal("- sign: " + config.signinCommand + "\n").formatted(net.minecraft.util.Formatting.AQUA));
                                        msgText = msgText.append(net.minecraft.text.Text.literal("- home: " + config.homeCommand + "\n").formatted(net.minecraft.util.Formatting.AQUA));
                                        msgText = msgText.append(net.minecraft.text.Text.literal("- 開箱物品欄位: " + config.handSlot + "\n").formatted(net.minecraft.util.Formatting.AQUA));
                                        msgText = msgText.append(net.minecraft.text.Text.literal("- 自動退出伺服器: " + (config.autoLeave ? "開啟" : "關閉") + "\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
                                        msgText = msgText.append(net.minecraft.text.Text.literal("- 自動放置物品: " + (config.autoPutItem ? "開啟" : "關閉") + "\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
                                        msgText = msgText.append(net.minecraft.text.Text.literal("- 自動進入伺服器: " + (config.autoServer ? "開啟" : "關閉") + "\n").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
                                        msgText = msgText.append(net.minecraft.text.Text.literal("- 自動轉IP: " + (config.autoChangeIp ? "開啟" : "關閉")).formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
                                        ctx.getSource().sendFeedback(msgText);
                                        return 1;
                                    })
                            )
                    );
        });
    }
}
