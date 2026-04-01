package com.concert.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.common.Constants;
import com.concert.entity.Concert;
import com.concert.entity.TicketTier;
import com.concert.entity.User;
import com.concert.mapper.ConcertMapper;
import com.concert.mapper.TicketTierMapper;
import com.concert.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final ConcertMapper concertMapper;
    private final TicketTierMapper ticketTierMapper;
    private final StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) {
            log.info("数据库已有数据，跳过初始化");
            return;
        }
        log.info("========== 开始初始化数据 ==========");
        initUsers();
        initConcerts();
        log.info("========== 数据初始化完成 ==========");
    }

    private void initUsers() {
        // 管理员
        insertUser("admin", "Admin@2024", "admin@concertgo.com", "13800000000", 1);

        // 10 个普通用户
        insertUser("zhangsan",  "User@1234", "zhangsan@qq.com",  "13900000001", 0);
        insertUser("lisi",      "User@1234", "lisi@qq.com",      "13900000002", 0);
        insertUser("wangwu",    "User@1234", "wangwu@qq.com",    "13900000003", 0);
        insertUser("zhaoliu",   "User@1234", "zhaoliu@qq.com",   "13900000004", 0);
        insertUser("sunqi",     "User@1234", "sunqi@qq.com",     "13900000005", 0);
        insertUser("zhouba",    "User@1234", "zhouba@qq.com",    "13900000006", 0);
        insertUser("wujiu",     "User@1234", "wujiu@qq.com",     "13900000007", 0);
        insertUser("zhengshi",  "User@1234", "zhengshi@qq.com",  "13900000008", 0);
        insertUser("chenyi",    "User@1234", "chenyi@qq.com",    "13900000009", 0);
        insertUser("yangling",  "User@1234", "yangling@qq.com",  "13900000010", 0);

        log.info("用户数据初始化完成: 1 管理员 + 10 普通用户");
    }

    private void insertUser(String username, String password, String email, String phone, int role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(Constants.USER_NORMAL);
        user.setLoginFailCount(0);
        userMapper.insert(user);
    }

    /**
     * 10 个城市、10 个艺人、半年内档期组合
     */
    private void initConcerts() {
        // 城市
        String[] cities = {
            "北京", "上海", "广州", "深圳", "成都",
            "杭州", "南京", "武汉", "重庆", "西安"
        };
        // 场馆（与城市对应）
        String[] venues = {
            "国家体育场（鸟巢）", "梅赛德斯-奔驰文化中心", "广州天河体育中心",
            "深圳湾体育中心", "成都露天音乐公园",
            "杭州奥体中心", "南京奥体中心", "武汉体育中心",
            "重庆奥体中心", "西安奥体中心"
        };
        // 艺人
        String[] artists = {
            "周杰伦", "林俊杰", "薛之谦", "陈奕迅", "五月天",
            "张学友", "邓紫棋", "华晨宇", "毛不易", "李荣浩"
        };
        // 巡演主题
        String[] tourNames = {
            "嘉年华世界巡回演唱会", "圣所世界巡回演唱会", "天外来物巡回演唱会",
            "FEAR AND DREAMS 世界巡回演唱会", "好好好想见到你巡回演唱会",
            "60+巡回演唱会", "I AM GLORIA 世界巡回演唱会", "火星演唱会",
            "小王巡回演唱会", "麻雀巡回演唱会"
        };
        // 每位艺人的演唱会海报图片（Unsplash 免费图片，演唱会/音乐现场主题）
        // 每个艺人 3 张不同风格的海报，与巡演主题视觉风格匹配
        String[][] posterUrls = {
            // 周杰伦 - 嘉年华：绚丽舞台灯光、大型演唱会现场、钢琴舞台
            {
                "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1578736641330-3155e606cd40?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&h=500&fit=crop"
            },
            // 林俊杰 - 圣所：蓝紫色灯光、梦幻舞台、音乐殿堂
            {
                "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=800&h=500&fit=crop"
            },
            // 薛之谦 - 天外来物：科幻感舞台、激光灯效、未来感
            {
                "https://images.unsplash.com/photo-1429962714451-bb934ecdc4ec?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=800&h=500&fit=crop"
            },
            // 陈奕迅 - FEAR AND DREAMS：暗调舞台、聚光灯、情感氛围
            {
                "https://images.unsplash.com/photo-1501612780327-45045538702b?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1415201364774-f6f0bb35f28f?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1504680177321-2e6a879aac86?w=800&h=500&fit=crop"
            },
            // 五月天 - 好好好想见到你：万人体育场、荧光棒海洋、热血现场
            {
                "https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=800&h=500&fit=crop"
            },
            // 张学友 - 60+：经典舞台、暖色灯光、大气演出
            {
                "https://images.unsplash.com/photo-1464375117522-1311d6a5b81f?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1499364615650-ec38552f4f34?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1565035010268-a3816f98589a?w=800&h=500&fit=crop"
            },
            // 邓紫棋 - I AM GLORIA：粉紫色舞台、动感灯光、流行风
            {
                "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1603228254119-e6a4d095dc59?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1598387993441-a364f854c3e1?w=800&h=500&fit=crop"
            },
            // 华晨宇 - 火星：红色火焰舞台、摇滚现场、震撼灯效
            {
                "https://images.unsplash.com/photo-1574391884720-bbc3740c59d1?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1508854710579-5cecc3a9ff17?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1563841930606-67e2bce48b78?w=800&h=500&fit=crop"
            },
            // 毛不易 - 小王：温暖民谣风、吉他舞台、柔和灯光
            {
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1485579149621-3123dd979885?w=800&h=500&fit=crop"
            },
            // 李荣浩 - 麻雀：吉他弹唱、简约舞台、音乐人氛围
            {
                "https://images.unsplash.com/photo-1446057032654-9d8885db76c6?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1471478331149-c72f17e33c73?w=800&h=500&fit=crop",
                "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=800&h=500&fit=crop"
            }
        };

        // 基准时间: 2026-02-15
        LocalDateTime baseDate = LocalDateTime.of(2026, 2, 15, 19, 30);

        // 生成 30 场演唱会：每个艺人 3 场，分布在不同城市
        // 档期从 2026.02 到 2026.08，间隔递增
        int concertIndex = 0;
        for (int artistIdx = 0; artistIdx < artists.length; artistIdx++) {
            // 每个艺人选 3 个不同城市
            int[] cityIndices = {
                artistIdx % cities.length,
                (artistIdx + 3) % cities.length,
                (artistIdx + 7) % cities.length
            };

            for (int round = 0; round < 3; round++) {
                int cityIdx = cityIndices[round];
                // 每场间隔 6 天，整体覆盖约 180 天
                LocalDateTime showTime = baseDate.plusDays((long) concertIndex * 6);

                // 状态：过去的已结束，近期的售票中，远期的未开始
                int status;
                if (showTime.isBefore(LocalDateTime.now().minusDays(1))) {
                    status = Constants.CONCERT_ENDED;
                } else if (showTime.isBefore(LocalDateTime.now().plusDays(30))) {
                    status = Constants.CONCERT_ON_SALE;
                } else {
                    status = Constants.CONCERT_NOT_STARTED;
                }

                Concert concert = new Concert();
                concert.setTitle(artists[artistIdx] + " " + tourNames[artistIdx] + " " + cities[cityIdx] + "站");
                concert.setDescription(artists[artistIdx] + tourNames[artistIdx]
                        + "，将于" + showTime.toLocalDate() + "在"
                        + cities[cityIdx] + venues[cityIdx] + "盛大开演。"
                        + "这是一场不容错过的音乐盛宴，届时将带来经典曲目与全新作品的精彩演绎。");
                concert.setArtist(artists[artistIdx]);
                concert.setVenue(venues[cityIdx]);
                concert.setCity(cities[cityIdx]);
                concert.setShowTime(showTime);
                concert.setPosterUrl(posterUrls[artistIdx][round]);
                concert.setStatus(status);
                concertMapper.insert(concert);

                // 为每场演唱会创建票档
                createTicketTiers(concert.getId(), artistIdx);

                concertIndex++;
            }
        }

        log.info("演唱会数据初始化完成: {} 场演唱会", concertIndex);
    }

    private void createTicketTiers(Long concertId, int artistIdx) {
        // 根据艺人热度设置不同价位
        // 顶流（周杰伦/张学友/五月天/陈奕迅）价格更高
        boolean isTopTier = (artistIdx == 0 || artistIdx == 3 || artistIdx == 4 || artistIdx == 5);

        BigDecimal vipPrice = isTopTier ? new BigDecimal("1880") : new BigDecimal("1280");
        BigDecimal normalPrice = isTopTier ? new BigDecimal("980") : new BigDecimal("680");
        BigDecimal economyPrice = isTopTier ? new BigDecimal("480") : new BigDecimal("380");

        insertTier(concertId, "SVIP内场票", vipPrice, 200);
        insertTier(concertId, "普通票", normalPrice, 1000);
        insertTier(concertId, "看台票", economyPrice, 2000);
    }

    private void insertTier(Long concertId, String tierName, BigDecimal price, int stock) {
        TicketTier tier = new TicketTier();
        tier.setConcertId(concertId);
        tier.setTierName(tierName);
        tier.setPrice(price);
        tier.setTotalStock(stock);
        tier.setAvailableStock(stock);
        ticketTierMapper.insert(tier);

        // 同步库存到 Redis
        redisTemplate.opsForValue().set(
                Constants.REDIS_STOCK_PREFIX + tier.getId(),
                String.valueOf(stock));
    }
}
