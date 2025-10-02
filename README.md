# 🛵💨 배로 (BAERO)

## 📌배달도, 배부름도 배로!

<img width="500" height="500" alt="Image" src="https://github.com/user-attachments/assets/5ea9bc86-32bc-4b21-b173-9e887daadea1" />


## 📝 프로젝트 개요
**배로는 ‘배달’을 더 빠르고 편리하게, ‘배부름’을 더 만족스럽게 만들어주는 배달 플랫폼입니다.**

- 원하는 음식을 쉽고 빠르게 찾고 주문할 수 있습니다.
- 조리부터 배달 완료까지 과정을 실시간으로 확인할 수 있습니다.
- 풍성한 혜택과 포인트로 한 끼의 만족을 배로 크게 느낄 수 있습니다.

---

## ⏳ 개발 기간
**2025.09.01-2025.10.03 (총 5주)**

---

## 👥 팀 구성
| 이름      | 구현 담당 기능 |
|--------|-----------|
| **최용현**  | Auth , OAuth 2.0 (Kakao & Naver), User, 이미지 저장 (AWS S3) |
| **배연주** | 통합 검색, 인기 검색어(Elasticsearch) |
| **이수빈** | 포인트, 쿠폰, 주문 현황 알림 (Slack API 연동), 관리자 권한 - 개인/전체 알림 (SSE) |
| **이연우**  | 가게, 사용자 주소 기반 가게 조회(Kakao REST API), 리뷰 및 댓글 |
| **지아현**  | 장바구니(Redis), 주문, 메뉴(옵션/카테고리) |

---

## 🏗 시스템 아키텍처
<img width="500" height="500" alt="Image" src="https://github.com/user-attachments/assets/c19a9f24-d8ea-49db-8d16-f09a6cf7ec4f" />

---

## 🎨 와이어프레임
[👉 와이어프레임(Figma)](https://embed.figma.com/design/tlgh158R8qiESF2e0RF0OO/%EC%A0%9C%EB%AA%A9-%EC%97%86%E[…]9mA52gwSjSOg7LD-1&embed-host=notion&footer=false&theme=system)
<img width="876" height="514" alt="스크린샷 2025-09-04 091716" src="https://github.com/user-attachments/assets/07832b99-b486-402f-9606-a36e62463c12" />

---

## 🗂 ERD
![Image](https://github.com/user-attachments/assets/df192527-3918-47f4-bada-4d7a54b7066c)
[👉 ERD 자세히 보기](https://github.com/user-attachments/files/22608618/ERD.pdf)

---


## 📌 사용자 Role

| Role            | 설명                                                                 |
|-----------------|----------------------------------------------------------------------|
| **ADMIN (관리자)** | 시스템 전체 관리 권한. 사용자/가게/쿠폰/알림 등 모든 리소스에 접근 가능 |
| **OWNER (오너)**   | 가게 운영자 권한. 본인 가게/메뉴/주문/리뷰 관리 가능                  |
| **USER (사용자)**  | 일반 사용자 권한. 주문, 리뷰 작성, 즐겨찾기, 포인트/쿠폰 사용 등 가능   |

---

## 🏗 사용자(고객) 흐름
<img width="755" height="993" alt="Image" src="https://github.com/user-attachments/assets/63eea544-4c9e-4e8d-b1cb-385ff1892059" />

---

## 🔗 API 설계
본 프로젝트의 API는 **RESTful 아키텍처 원칙**을 준수하여 설계되었습니다.  
리소스를 중심으로 한 일관된 경로 체계와 표준 HTTP 메서드(`GET`, `POST`, `PATCH`, `DELETE`)를 적용하여,  
클라이언트-서버 간 상호작용의 예측 가능성과 확장성을 높였습니다.

- **리소스 기반 설계**: `/users`, `/stores`, `/orders` 와 같이 명확한 리소스 중심 URL 구조
  
- **HTTP 메서드 매핑**:  
  - `GET`: 리소스 조회 (단건/목록)  
  - `POST`: 리소스 생성  
  - `PATCH`: 리소스 수정  
  - `DELETE`: 리소스 삭제 (Soft Delete 포함)  

- **일관성 있는 응답 포맷**: 모든 응답을 JSON 형식으로 제공  

- **예외 처리**: HTTP 상태 코드(2xx/4xx/5xx) 기반의 표준화된 에러 응답

📄 자세한 엔드포인트를 확인하고 싶다면? [👉API 명세서](https://www.notion.so/teamsparta/API-2612dc3ef51480b5819afeddde330366)

---

## 💾 데이터베이스 & 인증
- **데이터 매핑**
  -  Spring Data JPA와 MySQL 기반으로  
  엔티티(Entity)와 데이터베이스 테이블 간 매핑 자동화  

- **캐시/임시 저장소 (Redis)**  
  - 장바구니, 인기 검색어, 실시간 데이터 관리  

- **인증 방식 (JWT + Redis 하이브리드)**  
  - Access Token: 짧은 만료시간, 서버에서 서명 검증만 수행  
  - Refresh Token: Redis에 저장 → 토큰 회전/재발급, 강제 로그아웃 지원  
  - Redis를 통한 세션/블랙리스트 관리로 보안성 강화

---

## 🛠 기술 스택
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0.33-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS%20S3-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)
![Slack](https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=slack&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Bcrypt](https://img.shields.io/badge/Bcrypt-000000?style=for-the-badge&logo=letsencrypt&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-CA2C39?style=for-the-badge&logo=lombok&logoColor=white)
![Jackson](https://img.shields.io/badge/Jackson-000000?style=for-the-badge&logo=json&logoColor=white)
![Jakarta](https://img.shields.io/badge/Jakarta%20Annotation-007396?style=for-the-badge&logo=java&logoColor=white)
![Thymeleaf Extras](https://img.shields.io/badge/Thymeleaf%20Extras-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)

**Backend**
- Java 17  
- Spring Boot 3.5.5  
- Spring Web (REST API)  
- Spring Data JPA  
- Spring Security  
- Spring Mail  
- Spring Validation (Jakarta)  
- WebFlux   
- Thymeleaf

**Database & Cache**
- MySQL 8.0.33  
- Redis

**Infra & Storage**
- AWS S3
- Slack API Client  

**Search**
- Elasticsearch

**Auth & Encryption**
- JWT
- Spring Security Crypto  
- Bcrypt

**ETC**
- Lombok  
- Jackson
- Jakarta Annotations  
- Thymeleaf Extras 

---

## ✨ 주요 기능

<details>
<summary>🔔 실시간 알림</summary>

- **주문 현황 알림 (고객 User 대상)**
  - 주문 단계별 상태 변화 시 **자동 알림 전송**
  - 예: 주문 접수 → 조리 중 → 조리 완료 → 배달 중 → 배달 완료 / 취소, 거절
  - 일반 사용자(고객)는 앱을 통해 **실시간 주문 진행 상황 확인** 가능
  - 각 단계별 알림 내역은 **DB에 기록**되어 추후 조회 가능

- **관리자 알림 (Admin 권한)**
  - **특정 사용자 알림**: 관리자가 선택한 사용자에게 **개별 발송**
  - **전체 사용자 알림**: 모든 사용자에게 **동시에 발송**
  - **전달 방식: SSE(Server-Sent Events)** 활용 → 서버에서 클라이언트로 단방향 스트리밍

- **알림 로그 관리**
  - 관리자 권한으로 발송된 알림 이력 DB 저장
  - 성공/실패 여부 및 오류 메시지 확인 가능
  - 관리자 페이지에서 **전체 알림 로그 조회** 가능
</details>

<details>
<summary>🏪 가게 조회</summary>

- **부분 검색**: 입력한 키워드와 **부분 일치하는 가게도 조회 가능**
- **검색 필터링**
  - 주소 기반 반경 검색: 입력한 주소 → **지오코딩 변환** → 반경 내 가게 검색
  - 카테고리 필터: `분식`, `SNACK` 등 카테고리 이름으로 빠른 검색
- **전체 가게 목록 조회**
  - 모든 가게 확인 가능
  - 폐업한 가게 제외 → **최신 영업 중 가게만 조회**
- **가게 상세 조회**
  - 사용자: 주소, 영업 시간, 최소 주문 금액 등 확인
  - 오너: 본인 가게 관리 정보(메뉴, 폐업 여부 등) 확인
</details>

<details>
<summary>🔍 인기 검색어</summary>

- **사용자 검색 기록 저장**
  - 입력한 키워드 기록 → DB + Redis 저장
- **지역별 Top 10 인기 검색어**
  - Elasticsearch로 **지역 기준 집계 (1시간 단위)**
- **자동 완성 기능**
  - Elasticsearch `search_as_you_type` 기능 활용
  - 키워드 시작 부분 기준 추천 검색어 제공
  - 키워드(예: “피자”) + 지역(예: “서울”) 함께 입력 시 동작
</details>

<details>
<summary>🖼 이미지 저장</summary>

- **저장소**: AWS S3
- **업로드 방식 (Presigned URL 3-step)**
  1. **Presign 요청**: 클라이언트가 파일명·타입 전달 → 서버가 PUT URL 발급
  2. **클라이언트 업로드**: 발급받은 URL로 직접 업로드 → 서버 트래픽 최소화
  3. **업로드 확정(Confirm)**: 업로드 후 key·파일 정보 전달 → DB(`images` 테이블)에 메타데이터 저장
- **활용**
  - 가게(Store) 대표 이미지
  - 메뉴(Menu) 사진
  - 리뷰(Review) 첨부 이미지
</details>

---

## 📌 기술적 의사 결정

<details>
<summary>🛒 장바구니 구현</summary>

**🔒 요구 사항**
- 메뉴 담기/삭제/수량·옵션 변경 같은 **잦은 조회/수정을 빠르게 처리**
- 주문 전 **임시 저장** 용도
- 여러 기기에서 동일 계정으로 장바구니 확인 가능
- 대량 트래픽에도 **빠르고 안정적인 응답** 제공

**🔐 비교군**

1. **MySQL (관계형 DB)**
   - 🔵 장점  
     - 데이터 영속성 보장  
     - 복잡한 쿼리/조인 가능  
     - 트랜잭션 지원 → 무결성 관리 용이  
   - ❌ 단점  
     - 잦은 조회/수정 시 I/O 부담  
     - 임시 데이터라 불필요한 저장·관리 비용 증가  
     - 실시간 동기화 성능 한계  

2. **Redis (In-memory)**
   - 🔵 장점  
     - 읽기/쓰기 속도 매우 빠름  
     - TTL 지원 → 자동 삭제 가능  
     - userId 기반 멀티 디바이스 동기화  
     - 확장성 우수, 동시 접속 처리 강함  
   - ❌ 단점  
     - 데이터 영속성 제한 (스냅샷 필요)  
     - 메모리 비용 ↑  
     - 운영 시 클러스터링/모니터링 필요  

**🔑 결정 및 근거**
- **Redis에 저장**, 주문 생성 시 MySQL에 영구 저장  
- 빠른 속도 + TTL 최적화 + 멀티 디바이스 지원 + DB 부하 감소  
</details>

<details>
<summary>🏪 폐업한 가게 조회 포함 여부</summary>

**🔒 요구 사항**
- 사용자/비로그인은 **영업 중(ACTIVE) 가게만 조회 가능**  
- 페이지네이션/정렬은 **DB 기준**으로 정확해야 함  
- 성능상 불필요한 대량 Fetch 피해야 함  

**🔐 비교군**

1. **리포지토리 쿼리 필터링**
   - 🔵 장점  
     - DB 레벨에서 권한 경계 강제 → 폐업 가게 노출 차단  
     - DB가 직접 페이징/정렬 처리 → 정확성 ↑  
     - 조건 위반 조기 감지 가능  
     - 네트워크/메모리 사용 절감  
   - ❌ 단점  
     - 메서드 추가 필요할 수 있음  

2. **서비스 후처리**
   - 🔵 장점  
     - 초기 구현 단순  
   - ❌ 단점  
     - 필터 누락 시 폐업 가게 노출  
     - 페이징/정렬 왜곡  
     - 대량 Fetch → 성능 저하  

**🔑 결정 및 근거**
- **리포지토리 쿼리 필터링 채택**  
- DB 단계에서 권한 강제 → 안정성, 정확성, 성능 확보  
</details>

<details>
<summary>🔐 Auth</summary>

**🔒 요구 사항**
- Email 기반 회원가입/로그인  
- 소셜 로그인 지원  

**🔐 비교군**

1. **JWT + Redis**
   - 🔵 장점: 강제 로그아웃/토큰 폐기, 토큰 회전, 멀티 디바이스 관리 용이  
   - ❌ 단점: 인프라 복잡도 ↑, Redis 장애 영향  

2. **JWT**
   - 🔵 장점: 단순, 빠름, 수평 확장 유리  
   - ❌ 단점: 강제 무효화 어려움, 재사용 탐지 힘듦  

3. **Session**
   - 🔵 장점: 강제 무효화 쉬움, 구현 간단  
   - ❌ 단점: 확장성 비용, 모바일/SPA에 불리  

**🔑 결정 및 근거**
- **JWT + Redis 하이브리드**
  - Access 토큰: 짧게(10–30분), 서버 검증만
  - Refresh 토큰: 길게(7–30일), Redis 저장 & 회전
  - 기기별 세션 관리, 블랙리스트로 강제 무효화 지원
  - 보안성 + 확장성 균형  
</details>

<details>
<summary>🔍 인기 검색어</summary>

**🔒 요구 사항**
- 지역/시간/인기도 반영  
- 자주 변동되는 검색어 랭킹 유연 관리 필요  

**🔐 비교군**

1. **Completion suggester**
   - 🔵 장점: 초저지연 응답, 맞춤형 제안 가능  
   - ❌ 단점: 동적 업데이트 불편, 필터링 제약  

2. **Search_as_you_type**
   - 🔵 장점: 부분 단어 매칭, 동적 우선순위 반영, 확장성 유리  
   - ❌ 단점: 응답 속도는 Completion 대비 느림  

**🔑 결정 및 근거**
- **Search_as_you_type 채택**
- 지역/가게/메뉴와 결합 가능, 동적 인기도 반영, 확장성 유리  
</details>


---
## 🎥 시연 영상

[👉장바구니](https://www.youtube.com/watch?v=kp1dTQg0xqY)

[👉실시간 알림](https://youtu.be/QlV_FEmZYc4?si=KY2uVwRPXbilnS4I)

[👉인기 검색어]( https://youtu.be/ONjGBthskDY?si=6Rjwfuv29mB7yyFj)

---

## 🚀 실행 방법
```bash
# 1. 프로젝트 클론
git clone https://github.com/milk-abocado/final-project
cd final-project

# 2. 빌드 & 실행
./gradlew build
./gradlew bootRun
```

---

## 📏 Team Code Convention

### 기본 브랜치
- **main** : 배포용 브랜치  
- **dev** : 통합 개발 브랜치  

### 기능별 브랜치
- **auth** : 인증/인가 기능  
- **stores** : 가게 관리  
- **menus** : 메뉴 관리  
- **reviews** : 리뷰/댓글 기능  
- **orders** : 주문 기능  
- **carts** : 장바구니 기능  
- **coupons** : 쿠폰 기능  
- **point** : 포인트 기능  
- **slack-notification** : 실시간 알림 기능 (slack)  
- **searches1** : 통합 검색 / 인기 검색어 기능

### Commit Convention
- ✨ feat: 새로운 기능 추가
- 🎉 add: 신규 파일 생성 / 초기 세팅
- 🐛 fix: 버그 수정
- ♻️ refactor: 코드 리팩토링
- 🚚 move: 파일 이동/정리
- 🔥 delete: 기능/파일 삭제
- ✅ test: 테스트 코드 작성
- 🙈 gitfix: .gitignore 수정
- 🔨 script: build.gradle, docker compose 변경
- 📝 chore: 주석/변수명/클래스명 수정
- ⚡️ improve: 기능 개선
- 🔖 merge: 구현 기능 병합

---

## 📂 프로젝트 구조
```
├── config/ 
├── domain/
│ ├── auth
│ ├── carts
│ ├── common
│ ├── coupons
│ ├── elasticsearchpopular
│ ├── files
│ ├── menus
│ ├── notifications
│ ├── orders
│ ├── points
│ ├── reviews
│ ├── searches
│ ├── slack
│ ├── stores
│ └── users
└── FinalProjectApplication.java

src/main/resources/
├── application.properties
└── application.yml
```
---
