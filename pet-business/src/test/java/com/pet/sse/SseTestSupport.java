package com.pet.sse;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * SSE Broadcaster 测试脚手架（仅测试代码，不进入生产 classpath）。
 *
 * <p>为什么需要反射：三套 Broadcaster 的 {@code connect()} 内部直接 {@code new SseEmitter(0L)}，
 * 没有可注入的工厂，因此测试无法直接拿到/替换已注册的 emitter。这里提供两件能力：</p>
 * <ul>
 *   <li><b>读取/注入连接表</b>：反射访问 {@code emittersByUser}，用于断言注册、隔离与清理；</li>
 *   <li><b>触发容器回调</b>：反射取出 {@code completionCallback / timeoutCallback / errorCallback} 并执行，
 *       模拟 Tomcat 在真实容器里对连接完成、超时、出错的回调时机（{@code emitter.complete()} 在未初始化时
 *       不会触发 completionCallback，已实测确认）。</li>
 * </ul>
 *
 * <p>这两处都只依赖 Spring 公开类 {@code ResponseBodyEmitter} 的字段名，若升级 Spring 后字段改名，
 * 相关测试会以明确的反射异常失败，不会静默通过。</p>
 */
public final class SseTestSupport {

    private SseTestSupport() {
    }

    /** 取出 Broadcaster 内部的连接表（userId → emitters）。 */
    @SuppressWarnings("unchecked")
    public static Map<Long, List<SseEmitter>> registry(Object broadcaster) {
        try {
            Field f = broadcaster.getClass().getDeclaredField("emittersByUser");
            f.setAccessible(true);
            return (Map<Long, List<SseEmitter>>) f.get(broadcaster);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法读取 emittersByUser，Broadcaster 内部结构已变更", e);
        }
    }

    /** 已登记的用户数量（Map key 数）。 */
    public static int userCount(Object broadcaster) {
        return registry(broadcaster).size();
    }

    /** 指定用户当前的连接数量；用户不存在返回 0。 */
    public static int connectionCount(Object broadcaster, Long userId) {
        List<SseEmitter> list = registry(broadcaster).get(userId);
        return list == null ? 0 : list.size();
    }

    /** 把测试用 emitter 注入连接表，模拟「该用户已有一条连接」。 */
    public static void inject(Object broadcaster, Long userId, SseEmitter emitter) {
        registry(broadcaster)
                .computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>())
                .add(emitter);
    }

    /** 模拟容器触发 onCompletion。 */
    public static void fireCompletion(SseEmitter emitter) {
        runnableCallback(emitter, "completionCallback").run();
    }

    /** 模拟容器触发 onTimeout。 */
    public static void fireTimeout(SseEmitter emitter) {
        runnableCallback(emitter, "timeoutCallback").run();
    }

    /** 模拟容器触发 onError。 */
    public static void fireError(SseEmitter emitter, Throwable error) {
        try {
            Field f = ResponseBodyEmitter.class.getDeclaredField("errorCallback");
            f.setAccessible(true);
            Object cb = f.get(emitter);
            if (cb == null) {
                throw new IllegalStateException("emitter 未注册 errorCallback");
            }
            @SuppressWarnings("unchecked")
            Consumer<Throwable> consumer = (Consumer<Throwable>) cb;
            consumer.accept(error);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法触发 errorCallback", e);
        }
    }

    private static Runnable runnableCallback(SseEmitter emitter, String fieldName) {
        try {
            Field f = ResponseBodyEmitter.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object cb = f.get(emitter);
            if (cb == null) {
                throw new IllegalStateException("emitter 未注册 " + fieldName);
            }
            return (Runnable) cb;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法触发 " + fieldName, e);
        }
    }

    /**
     * 未初始化 emitter 上已缓冲的**事件数量**。
     *
     * <p>为什么不能直接数 {@code earlySendAttempts} 的元素个数：Spring 把一个 SSE 事件拆成多段
     * （"event:xxx\ndata:" 协议头字符串 + 业务载荷对象），一次 send 会往集合里放多条记录，
     * 数量随 Spring 内部 flush 策略变化。这里改为统计含 {@code event:} 前缀的协议头条目，
     * 每次 send 恰好一条，因此等于逻辑事件数。</p>
     */
    public static int bufferedEventCount(SseEmitter emitter) {
        try {
            Field f = ResponseBodyEmitter.class.getDeclaredField("earlySendAttempts");
            f.setAccessible(true);
            Object set = f.get(emitter);
            if (set == null) {
                return 0;
            }
            int events = 0;
            for (Object item : (java.util.Collection<?>) set) {
                Object data = ((ResponseBodyEmitter.DataWithMediaType) item).getData();
                if (data instanceof CharSequence && data.toString().contains("event:")) {
                    events++;
                }
            }
            return events;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法读取 earlySendAttempts", e);
        }
    }

    /** 未初始化 emitter 上已缓冲的业务载荷数量（= 逻辑事件数）。 */
    public static int bufferedSendCount(SseEmitter emitter) {
        return bufferedEventCount(emitter);
    }

    /**
     * 可观测的 SseEmitter：记录每次发送的事件名与业务载荷，并可在下一次发送时抛 IOException。
     *
     * <p>三套 Broadcaster 都用 {@code emitter.send(SseEmitter.event()...)} 这一个重载，
     * 因此只覆写 {@link #send(SseEmitter.SseEventBuilder)} 即可完整拦截。</p>
     */
    public static class CapturingSseEmitter extends SseEmitter {

        /** 每次 send 记录下来的业务载荷（协议头字符串不记录）。 */
        private final List<Object> payloads = new ArrayList<>();
        /** 每次 send 记录下来的事件名（从 "event:xxx" 协议头解析）。 */
        private final List<String> eventNames = new ArrayList<>();
        /** 记录下来的 SSE 注释帧（心跳用，以 ':' 开头）。 */
        private final List<String> comments = new ArrayList<>();
        private int sendCount;
        private final AtomicBoolean failNextSend = new AtomicBoolean(false);
        /** true = 下次失败抛 IllegalStateException（模拟已 complete 的 emitter），false = IOException。 */
        private final AtomicBoolean failWithIllegalState = new AtomicBoolean(false);

        public CapturingSseEmitter() {
            super(0L);
        }

        /** 让下一次 send 抛 IOException，用于验证「管道断开即移除连接」。 */
        public void failNextSendWithIOException() {
            failWithIllegalState.set(false);
            failNextSend.set(true);
        }

        /** 让下一次 send 抛 IllegalStateException，用于验证「已完成连接不再写入且不中断其它连接」。 */
        public void failNextSendWithIllegalState() {
            failWithIllegalState.set(true);
            failNextSend.set(true);
        }

        public List<Object> payloads() {
            return payloads;
        }

        public List<String> eventNames() {
            return eventNames;
        }

        public List<String> comments() {
            return comments;
        }

        public int sendCount() {
            return sendCount;
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (failNextSend.compareAndSet(true, false)) {
                if (failWithIllegalState.get()) {
                    throw new IllegalStateException("ResponseBodyEmitter has already completed");
                }
                throw new IOException("simulated broken pipe");
            }
            sendCount++;
            for (ResponseBodyEmitter.DataWithMediaType part : builder.build()) {
                Object data = part.getData();
                if (data instanceof CharSequence text) {
                    String s = text.toString();
                    int idx = s.indexOf("event:");
                    if (idx >= 0) {
                        String name = s.substring(idx + "event:".length()).split("\n")[0].trim();
                        eventNames.add(name);
                    } else if (s.startsWith(":")) {
                        comments.add(s);
                    }
                    continue;
                }
                payloads.add(data);
            }
        }

        @Override
        public void send(Object object, MediaType mediaType) throws IOException {
            sendCount++;
            payloads.add(object);
        }
    }
}
