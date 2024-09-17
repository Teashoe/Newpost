package com.teashoe.newposts;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.text.MutableText;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.util.ActionResult;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class Newposts implements ClientModInitializer {

    private boolean newPostAlertEnabled = true;
    private final Set<String> currentPostNumbers = new HashSet<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(); // 작업 스케줄러 추가

    @Override
    public void onInitializeClient() {
        // 설정 파일을 등록하여 초기화
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);

        AutoConfig.getConfigHolder(ModConfig.class).registerSaveListener((configHolder, newConfig) -> {
            initializePostNumbers(); // 설정이 변경될 때마다 기존 게시물 목록 초기화
            return ActionResult.SUCCESS;
        });

        // 서버에 접속할 때 기존 게시물 목록을 초기화
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> initializePostNumbers());

        // 클라이언트 틱 이벤트에 알림 기능 등록
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (newPostAlertEnabled && !scheduler.isShutdown()) {
                // 주기적으로 웹 요청 작업을 수행하도록 설정 (60초 간격)
                scheduler.scheduleAtFixedRate(() -> checkNewPosts(client), 0, 60, TimeUnit.SECONDS);
            }
        });
    }

    private void initializePostNumbers() {
        // 설정 파일에서 galleryId를 가져옴
        String galleryId = ModConfig.get().galleryId; // 사용자 정의 값 사용
        String url = "https://gall.dcinside.com/mgallery/board/lists?id=" + galleryId;

        try {
            Document document = Jsoup.connect(url).get();
            Elements postList = document.select(".ub-content.us-post");

            for (Element postElement : postList) {
                String number = postElement.select(".gall_num").text();
                currentPostNumbers.add(number); // 기존 게시물 번호를 저장
            }
        } catch (IOException e) {
            System.out.println("게시물을 초기화하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void checkNewPosts(MinecraftClient client) {
        if (client.player == null) return; // 플레이어가 없으면 중지

        // 설정 파일에서 galleryId를 가져옴
        String galleryId = ModConfig.get().galleryId; // 사용자 정의 값 사용
        String url = "https://gall.dcinside.com/mgallery/board/lists?id=" + galleryId;

        try {
            Document document = Jsoup.connect(url).get();
            Elements postList = document.select(".ub-content.us-post");

            for (Element postElement : postList) {
                String number = postElement.select(".gall_num").text();
                if (!currentPostNumbers.contains(number)) {
                    currentPostNumbers.add(number);
                    String title = postElement.select(".gall_tit.ub-word").text();
                    String author = postElement.select(".gall_writer.ub-writer .nickname em").text();

                    // "[새 게시물]" 부분을 노란색으로 설정하고 MutableText로 변환
                    MutableText newPostPrefix = Text.literal("[새 게시물] ")
                            .styled(style -> style.withColor(Formatting.YELLOW));

                    // 제목과 작성자 부분을 기본 색상으로 설정
                    Text postDetails = Text.literal(title + " [" + author + "]")
                            .styled(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                            "https://gall.dcinside.com/mgallery/board/view/?id=" + galleryId + "&no=" + number))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("게시물 보기")))
                                    .withColor(Formatting.WHITE)
                            );

                    // 두 텍스트를 결합
                    Text clickableMessage = newPostPrefix.append(postDetails);

                    // 클라이언트 플레이어에게 메시지를 전송 (메인 스레드에서 실행해야 함)
                    client.execute(() -> client.player.sendMessage(clickableMessage, false));
                    client.player.playSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
            }

        } catch (IOException e) {
            // 오류 메시지를 클라이언트 플레이어에게 전송
            client.execute(() -> client.player.sendMessage(Text.literal("게시물을 가져오는 중 오류가 발생했습니다."), false));
            e.printStackTrace();
        }
    }
}
