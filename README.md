# ⚡️ Binance HFT Server

> - **Java/Spring Boot 기반의 Binance Spot 비트코인 고빈도 매매 시스템**
> - 호기심과 기술적 도전을 위해 시작된 개인 프로젝트로, Binance의 Zero-fee 페어에서 발생하는 유동성 불균형을 포착해 초단타 스캘핑을 수행합니다.

![Java](https://img.shields.io/badge/Java_21-007396?style=flat-square&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5.8-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle_8.14.3-02303A?style=flat-square&logo=gradle&logoColor=white)

---

## 🎥 시연 영상

[![프로젝트 시연 영상](https://img.youtube.com/vi/haIsjBmVU30/maxresdefault.jpg)](https://www.youtube.com/watch?v=haIsjBmVU30)

---

## 💡 주요 매매 전략

본 시스템은 **Binance의 FDUSD 페어(`BTCFDUSD`) 지정가 매매 시 수수료가 0원**이라는 점을 활용합니다. 풍부한 유동성 사이에서 호가창의 미세한 틈을 노려 작지만 지속적인 수익을 창출하는 것을 목표로 합니다.

1. **매수**
   - 실시간 WebSocket 호가 데이터를 분석하여, 일정 규모 이상의 매수 벽(Buy Wall)이 감지되면 가장 높은 매수 호가보다 `$0.01` 높은 가격에 지정가 매수 주문을 실행해 체결 우선순위를 선점합니다.
2. **매도**
   - 매수 체결 이벤트가 발생하는 즉시 매수 평균가의 `+0.01%` 마진을 목표가로 설정합니다.
   - 단, 실시간 시장의 최우선 매도 호가가 목표가보다 높게 형성되어 있다면 그에 맞춰 더 높은 가격으로 매도 주문을 실행해 수익을 극대화합니다.
3. **리스크 최소화**
   - 기존에 체결 대기 중인 주문들의 가격과 신규 진입 가격을 지속적으로 비교/검사하여, 가격이 겹치는 중복 및 비효율적인 주문 진입을 사전에 방지합니다.

---

## 🚀 핵심 기능

### 1. 실시간 호가 및 체결 데이터 스트리밍
REST API의 폴링 지연을 없애기 위해 WebSocket을 통해 데이터를 수신합니다.
* `MarketDataStream`: 100ms 단위의 실시간 호가 데이터 수신
* `UserDataStream`: 계좌 잔고 변화 및 내 주문의 체결 상태 실시간 데이터 수신

### 2. 미체결 주문 관리
Binance는 시스템 안정성을 위해 최대 200개의 미체결 주문만 허용합니다. (본 시스템은 안전을 위해 190개로 제한)
* **스마트 복구 시스템:** 미체결 주문 한도에 도달하면 가장 가격 경쟁력이 떨어지는 주문을 취소하여 우선순위 큐(`PriorityBlockingQueue`)에 캐싱해 둡니다. 이후 주문 슬롯에 여유가 생기면 저장해둔 주문을 다시 복구하여 기회 손실을 방지합니다.

### 3. 동시성 제어 및 인메모리 관리
초당 수십 번의 데이터가 들어오는 HFT 환경에 맞춰, 별도의 외부 DB 없이 애플리케이션 메모리 내에서 상태를 관리합니다.
* `ConcurrentHashMap`을 활용한 안전한 주문 상태 관리
* 가장 최근에 종료된 주문을 추적하기 위한 LRU(Least Recently Used) 방식 구현

---

## 🛠 세팅

이 프로젝트를 로컬에서 실행하려면 Binance API 발급 및 설정이 필요합니다. 보안상 Ed25519 알고리즘 방식을 지원하도록 구성되어 있습니다.

프로젝트 루트 디렉토리에 `secrets` 폴더를 생성하고, 두 개의 파일을 준비합니다.

**📁 `./secrets/private_key.pem`** 생성:

```text
-----BEGIN PRIVATE KEY-----
YOUR PRIVATE KEY
-----END PRIVATE KEY-----
```

**📁 `./secrets/secrets.yaml`** 생성:
```yaml
hft:
  exchange:
    api-key: "YOUR API KEY"
```

## ⚙️ 커스터마이징

`src/main/resources/application.yaml` 파일 내의 속성을 수정하여 매매 전략의 세부 파라미터와 리스크 관리 수치를 본인의 전략에 맞게 조율할 수 있습니다.

```yaml
hft:
  stream:
    # depth는 5, 10, 20 중 선택 가능하며, 속도는 100ms 또는 1000ms로 조합할 수 있습니다.
    market-uri: wss://data-stream.binance.vision:443/ws/btcfdusd@depth5@100ms

  trading:
    symbol: BTCFDUSD
    min-order-size: "5"                         # 최소 주문 금액 (USD 기준)
    
    risk:
      max-open-orders: "190"                    # 최대 미체결 주문 개수 (Binance 정책 대비 안전망)
      max-buy-orders: "1"                       # 유지할 미체결 매수 주문 개수
      max-sell-orders: "100"                    # 유지할 최대 미체결 매도 주문 개수
      min-sell-orders: "90"                     # 미체결 매도 주문 개수 유지를 위한 하한선
      buy-wall-threshold-usd: "1000"            # 매수 벽 판단 기준 금액
      target-multiplier: "1.0001"               # 매도 마진율 (1.0001 = 매수가의 +0.01%)
      price-conflict-tolerance-rate: "0.000005" # 매수 주문 가격 범위 지정 (비슷한 가격대 중복 매수 방지)
```

---

## ⚠️ 주의 사항

이 프로젝트는 개인적인 기술 연구와 자동매매 시스템 구현에 대한 호기심으로 시작된 개인 프로젝트입니다.

제공된 코드는 실제 시장에서의 완벽한 동작이나 안정적인 수익을 보장하지 않습니다.

---
