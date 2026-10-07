# Study

## application.properties 프로파일 분리

### 구조

- `application.properties` — 공통 설정 (항상 읽힘, 커밋 O)
- `application-local.properties` — 로컬 환경 설정 (profile이 `local`일 때 추가로 읽힘, 커밋 X)

### 동작 방식

`application.properties`는 **무조건** 읽히고, `application-{profile}.properties`는 해당 profile이 활성화될 때 **추가로** 읽힌다. 중복된 설정이 있으면 profile 파일이 덮어쓴다.

### profile 활성화 방법

1. `application.properties`에 `spring.profiles.active=local` 추가
2. IntelliJ Run Configuration에서 `Active profiles`에 `local` 입력

둘 중 하나만 하면 된다.

### 왜 분리하는가?

DB 비밀번호 같은 민감 정보를 GitHub에 올리지 않기 위해서다. `application-local.properties`를 `.gitignore`에 추가하면 로컬 환경 정보가 커밋되지 않는다.

### 실무 패턴

| 파일 | 용도 | 커밋 |
|------|------|------|
| `application.properties` | 공통 설정 | O |
| `application-local.properties` | 로컬 DB 등 | X |
| `application-dev.properties` | 개발 서버 | X 또는 환경변수 |
| `application-prod.properties` | 운영 서버 | X 또는 환경변수 |

---

## JpaConfig (@EnableJpaAuditing)

### 역할

`created_at`, `updated_at` 같은 공통 컬럼을 매번 수동으로 넣지 않고 JPA가 자동으로 채워주게 하는 기능(JPA Auditing)이다. `@EnableJpaAuditing`이 이 기능을 활성화하는 스위치이며, 이 설정이 없으면 `@CreatedDate`, `@LastModifiedDate` 어노테이션을 붙여도 동작하지 않는다.

### 설정 클래스

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

### BaseTimeEntity

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

#### 클래스 레벨
- `abstract` — 단독으로 사용할 일이 없고 상속 전용이므로 추상 클래스로 선언. `new BaseTimeEntity()` 실수를 컴파일 시점에 방지
- `@MappedSuperclass` — 이 클래스 자체는 테이블이 안 만들어지고, 상속한 엔티티에 필드가 포함됨
- `@EntityListeners(AuditingEntityListener.class)` — JPA Auditing 리스너 등록. 이게 있어야 `@CreatedDate`, `@LastModifiedDate`가 동작함

#### 필드 레벨
- `@CreatedDate` — 엔티티가 최초 저장(`save`)될 때 현재 시간 자동 설정. Spring Data JPA 내장 어노테이션 (`org.springframework.data.annotation`)
- `@LastModifiedDate` — 엔티티가 수정될 때마다 현재 시간 자동 갱신. 마찬가지로 Spring Data JPA 내장
- `@Column(updatable = false)` — `createdAt`은 UPDATE 쿼리에서 제외. 수정 시 변경되지 않도록 보호
- `private LocalDateTime` — 자식에서 직접 접근 불가. getter로 접근

#### 동작 흐름
```
save() 호출 → AuditingEntityListener가 감지 → createdAt, updatedAt 자동 설정
수정 후 save() → updatedAt만 갱신 (createdAt은 updatable = false라 그대로)
```

---

## Docker Compose로 MySQL 실행

### 명령어

```bash
docker-compose up -d
```

- `up` — `docker-compose.yml`에 정의된 컨테이너를 생성하고 실행
- `-d` — detached 모드 (백그라운드 실행). 없으면 터미널에 로그가 계속 출력됨

`docker-compose.yml`이 있는 디렉토리에서 실행해야 한다.

### 기타 명령어

| 명령어 | 설명 |
|--------|------|
| `docker-compose up -d` | 컨테이너 생성 및 백그라운드 실행 |
| `docker-compose down` | 컨테이너 중지 및 삭제 |
| `docker-compose ps` | 실행 중인 컨테이너 목록 |
| `docker-compose logs -f` | 로그 실시간 확인 |

### 주의사항

Docker Desktop이 실행 중이어야 한다. 실행되지 않으면 `Cannot connect to the Docker daemon` 에러가 발생한다.

---

## Java record

Java 14에서 추가된 기능으로, **불변 데이터 객체**를 간결하게 만드는 문법이다. DTO처럼 데이터만 담는 객체에 적합하다.

### 비교

```java
// record — 한 줄로 끝
public record ApiResponse<T>(boolean success, T data, ErrorResponse error) {}

// 동일한 class — 생성자, getter, equals, hashCode, toString 직접 작성 필요
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    public ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean success() { return success; }
    public T data() { return data; }
    public ErrorResponse error() { return error; }
    // equals, hashCode, toString도 자동 생성
}
```

### 자동 생성되는 것
- 생성자 (모든 필드를 받는)
- getter (필드명과 동일한 메서드: `success()`, `data()` 등)
- `equals`, `hashCode`, `toString`

### 특징
- 필드가 `final`이라 한번 만들면 수정 불가 (불변)
- Java 14부터 지원, 현재 프로젝트(Java 17)에서 사용 가능

### record vs @Data (Lombok)

| | `record` | `@Data` (Lombok) |
|--|----------|-----------------|
| 불변 | O (`final`) | X (setter 포함) |
| 외부 라이브러리 | 불필요 (Java 내장) | Lombok 필요 |
| setter | 없음 | 자동 생성 |
| 상속 | 불가 | 가능 |

`record`는 불변 객체, `@Data`는 가변 객체이다. 보통 응답 DTO → `record`, 엔티티 → `@Getter` + `@Setter` 로 구분해서 사용한다.

### 왜 불변으로 만드는가?

응답 DTO는 만들어진 후에 값이 바뀔 이유가 없다. 서버에서 응답을 만들고 → 클라이언트에 전달하는 게 끝이라, 중간에 값이 바뀌면 오히려 버그다. 불변으로 만들면 의도치 않은 수정을 컴파일 시점에 막아준다.

```java
ApiResponse<String> response = new ApiResponse<>(true, "data", null);
// response.setSuccess(false); ← setter가 없으므로 불가능
```

### 불변 = `new`로 만들 때 값을 넣고, 이후엔 못 바꾼다

> **"record 불변 객체는 new로 만들고 값 변경이 안 되는 거지?" → 맞다.** 생성 시점에 생성자로 값을 한 번에 넣고, 그 뒤로는 바꿀 수 없다.

```java
JwtProperties props = new JwtProperties("exjP...", 1800000, 604800000, "cafe-reservation");
//                    ↑ 만들 때 생성자로 값을 한 번에 넣음

props.secret();                 // ✅ 꺼내기는 가능
props.secret = "다른값";         // ❌ 컴파일 에러 — 필드가 private final
props.setSecret("다른값");       // ❌ 컴파일 에러 — setter가 없음
```

- record 필드는 자동으로 **`private final`** → 생성자에서 한 번 넣은 뒤 재대입 불가
- **setter를 만들어주지 않는다** → 접근자(`secret()`)만 있음

#### 다른 값이 필요하면 → 새로 만든다

```java
JwtProperties changed = new JwtProperties("새secret", props.accessTokenExpiration(), ...);
// 기존 props는 그대로, 새 객체가 하나 더 생김
```

기존 객체를 고치는 게 아니라 **바뀐 값으로 새 객체를 만든다.** `String`이 불변이라 `str.toUpperCase()`가 원본을 바꾸지 않고 새 문자열을 돌려주는 것과 같은 방식이다.

#### `new`를 꼭 내가 부르는 건 아니다

`JwtProperties`는 코드에서 `new`를 쓴 적이 없다. Spring이 설정 파일 값을 읽어 **대신 `new JwtProperties(...)`를 호출**한다 (생성자 바인딩 → "JwtProperties" 섹션). 누가 만들든 **생성 시점에 값이 정해지고 이후 못 바꾸는 건** 같다.

#### 주의: "얕은" 불변이다

필드 자체를 다른 값으로 바꾸는 건 막지만, 필드가 `List`처럼 **안쪽 내용을 바꿀 수 있는 객체**면 그 내용은 바뀔 수 있다.

```java
record Order(List<String> items) {}

Order order = new Order(new ArrayList<>(List.of("커피")));
order.items() = new ArrayList<>();   // ❌ 불가 — 필드 재대입
order.items().add("케이크");          // ⚠️ 가능 — 리스트는 그대로인데 안의 내용이 바뀜
```

| 필드 타입 | 완전히 불변? |
|---|---|
| `String`, `long`, `boolean`, `Integer`, enum | O — 원래부터 바뀌지 않는 타입 |
| `ArrayList`, 배열, 일반 클래스 객체 | X — 안쪽 내용은 바뀔 수 있음 |

`ErrorResponse`, `JwtProperties`는 바뀌지 않는 타입만 써서 해당 없다. `ApiResponse<T>`는 `T`에 `List<MenuResponse>`처럼 리스트가 들어갈 수 있지만, 응답을 만들고 바로 JSON으로 보내고 끝나서 실제로 문제 될 일은 거의 없다. 리스트를 오래 들고 있는 record를 만들 때는 `List.of(...)`(불변 리스트)를 넣거나, 생성자에서 `List.copyOf(...)`로 복사해두면 완전히 불변이 된다.

---

## 제네릭 (Generic)

어떤 타입이든 받을 수 있게 해주는 문법이다. 하나의 클래스를 여러 타입에 공통으로 쓸 수 있다.

### `<T>` 선언과 사용

```java
public record ApiResponse<T>(boolean success, T data, ErrorResponse error) {}
//                       ↑ 선언 ("T라는 타입 변수를 쓰겠다")
//                                          ↑ 사용
```

선언 없이 `T data`만 쓰면 컴파일러는 `T`가 뭔지 모른다. `T`라는 이름의 클래스를 찾다가 에러가 난다.

### 사용 예시

```java
ApiResponse<String> response1 = new ApiResponse<>(true, "hello", null);
ApiResponse<OrderResponse> response2 = new ApiResponse<>(true, orderResponse, null);
ApiResponse<List<MenuResponse>> response3 = new ApiResponse<>(true, menuList, null);
```

`T`에 아무 타입이나 들어올 수 있어서, 모든 API에서 하나의 `ApiResponse`를 공통으로 쓸 수 있다. `T` 없이 만들면 API마다 별도의 응답 클래스를 만들어야 한다.

### 제네릭 선언 vs 타입 지정

- **선언** — 클래스/메서드에서 `<T>` 처럼 타입 변수를 정의하는 것
- **타입 지정** — 선언된 `<T>`에 구체적인 타입을 넣는 것

```java
// 선언 — "T라는 타입 변수를 쓰겠다"
public record ApiResponse<T>(...) {}

// 타입 지정 — T에 Void를 넣겠다
ResponseEntity<ApiResponse<Void>>
```

### ResponseEntity도 제네릭

`ResponseEntity`는 HTTP 응답(상태코드 + body)을 감싸는 Spring 클래스이며, `<>` 안에 body에 담길 타입을 지정한다.

```java
ResponseEntity<String>              // body가 String
ResponseEntity<ApiResponse<Void>>   // body가 ApiResponse<Void>
```

### ResponseEntity 구조

`ResponseEntity`가 상태 코드 + 헤더를 담당하고, body에 `ApiResponse`가 들어가서 JSON으로 변환된다.

```
ResponseEntity
├── 상태 코드 (HttpStatus)  → 200, 400, 500 등
├── 헤더 (HttpHeaders)      → Content-Type 등
└── body (T)                → ApiResponse<Void>
    ├── success             → false
    ├── data (Void)         → null
    └── error               → ErrorResponse
        ├── code            → "COMMON_002"
        └── message         → "서버 내부 오류가 발생했습니다."
```

실제 JSON 응답:
```json
// HTTP 500
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_002",
    "message": "서버 내부 오류가 발생했습니다."
  }
}
```

`Void`로 타입 지정한 이유는 에러 응답에서 `data`에 넣을 값이 없어서 `null`을 넣기 때문이다. 성공 응답은 데이터가 있으므로 해당 타입을 지정한다 (예: `ApiResponse<OrderResponse>`).

### static 메서드에서의 제네릭

`static` 메서드에서는 클래스에 선언된 `<T>`를 쓸 수 없다. 메서드 자체에 `<T>`를 별도로 선언해야 한다.

```java
// 잘못된 예 — static 메서드에서 클래스의 T를 쓸 수 없음
public static ResponseEntity<ApiResponse<T>> ok(T data) { ... } // 컴파일 에러

// 올바른 예 — 메서드 레벨에서 <T> 별도 선언
public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
    return ResponseEntity.ok(new ApiResponse<>(true, data, null));
}
```

이유: `static`은 인스턴스 없이 호출되므로 인스턴스 생성 시 결정되는 클래스의 `<T>`를 알 수 없다. 유동적으로 `T`를 받는데 인스턴스가 없으니 메서드 앞에 `<T>`를 별도 선언해야 한다.

반면 `fail`은 반환 타입이 `ApiResponse<Void>`로 고정이라 `<T>` 선언이 필요 없다.

```java
// fail — Void 고정, <T> 불필요
public static ResponseEntity<ApiResponse<Void>> fail(ErrorCode code) { ... }

// ok — 호출마다 타입이 달라짐, <T> 필요
public static <T> ResponseEntity<ApiResponse<T>> ok(T data) { ... }
```

### 반환 타입 일치

반환 타입이 일치하면 그대로 return 가능하다:

```java
// fail의 반환 타입: ResponseEntity<ApiResponse<Void>>
public static ResponseEntity<ApiResponse<Void>> fail(ErrorCode code) { ... }

// handleException의 반환 타입도 동일하므로 바로 return 가능
public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    return ApiResponse.fail(ErrorCode.INTERNAL_ERROR);
}
```

---

## @Getter, @RequiredArgsConstructor (Lombok)

### @Getter
모든 필드의 getter를 자동 생성한다.

### @RequiredArgsConstructor
`final` 필드만 받는 생성자를 자동 생성한다. 필드 선언 순서대로 매칭된다.

### enum에서의 사용 예시

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_CREDENTIALS("AUTH_001", "아이디 또는 비밀번호가 일치하지 않습니다.", 401);
//  ↑ enum 상수 (고정된 인스턴스)  ↑ 생성자에 넘기는 값 (필드 선언 순서대로 매칭)

    private final String code;    // 1번째 → "AUTH_001"
    private final String message; // 2번째 → "아이디 또는 비밀번호가 일치하지 않습니다."
    private final int status;     // 3번째 → 401
}
```

```java
// 사용
ErrorCode.INVALID_CREDENTIALS.getCode()    // "AUTH_001"
ErrorCode.INVALID_CREDENTIALS.getMessage() // "아이디 또는 비밀번호가 일치하지 않습니다."
ErrorCode.INVALID_CREDENTIALS.getStatus()  // 401
```

- `INVALID_CREDENTIALS`는 메서드가 아니라 enum **상수** (ErrorCode 타입의 고정된 인스턴스). 내부적으로 `new ErrorCode("AUTH_001", "...", 401)`과 같지만, enum은 `new`로 직접 생성할 수 없고 선언된 상수만 사용 가능
- `@RequiredArgsConstructor` 덕분에 `("AUTH_001", "...", 401)` 형태로 값을 넣을 수 있다
- `@Getter` 덕분에 `getCode()`, `getMessage()`, `getStatus()`를 직접 작성하지 않아도 된다

---

## BusinessException

비즈니스 로직에서 발생하는 예외를 공통으로 처리하기 위한 커스텀 예외 클래스다.

### 코드

```java
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

### 설명

- `extends RuntimeException` — 언체크 예외라서 `throws` 선언 없이 어디서든 던질 수 있다
- `@Getter` — `e.getErrorCode()`로 ErrorCode enum을 꺼낼 수 있다

### 생성자 동작

`new BusinessException(ErrorCode.ORDER_NOT_FOUND)` 하면 실행된다.

```java
public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());  // 1. 부모(RuntimeException)에 "주문을 찾을 수 없습니다." 전달
    this.errorCode = errorCode;     // 2. 나중에 getErrorCode()로 꺼내 쓸 수 있도록 저장
}
```

- `super(...)` — 부모 클래스의 생성자 호출. `RuntimeException`에는 여러 생성자가 있고, 그 중 `RuntimeException(String message)`를 호출하는 것. 여기에 넣은 메시지가 `e.getMessage()`로 꺼낼 수 있게 됨

```java
// RuntimeException의 생성자들
public RuntimeException() {}
public RuntimeException(String message) {}           // ← 이걸 호출
public RuntimeException(String message, Throwable cause) {}
public RuntimeException(Throwable cause) {}
```
- `this.errorCode = errorCode` — 필드에 저장. `GlobalExceptionHandler`에서 `e.getErrorCode()`로 꺼내서 상태코드, 에러코드 등을 응답에 사용

### BusinessException이 가지는 필드

`RuntimeException`을 상속하고 `super(message)`를 호출하므로, `message`(부모)와 `errorCode`(자신) 둘 다 가진다.

```java
BusinessException e = new BusinessException(ErrorCode.ORDER_NOT_FOUND);

e.getMessage()   // "주문을 찾을 수 없습니다." (RuntimeException에서 상속받은 필드)
e.getErrorCode() // ErrorCode.ORDER_NOT_FOUND   (BusinessException 자체 필드)
```

### 왜 super에 message를 넣는가?

ErrorCode에 이미 message가 있지만, 로그 프레임워크, 스택 트레이스, Spring 기본 에러 처리 등에서 자동으로 `e.getMessage()`를 호출한다. `super`에 안 넣으면 `null`이 나온다.

```
// super에 넣었을 때 로그
BusinessException: 주문을 찾을 수 없습니다.

// super에 안 넣었을 때 로그
BusinessException: null
```

`e.getErrorCode().getMessage()`로도 꺼낼 수 있지만, 기존 자바 예외 체계와 호환되려면 `super`에도 넣어두는 게 관례다.

### 사용 흐름

```java
// Service에서 던지고
throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);

// GlobalExceptionHandler에서 잡는다
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
    return ApiResponse.fail(e.getErrorCode()); // errorCode에서 code, message, status 꺼냄
}
```

---

## JPA 엔티티 (User)

### 코드

```java
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @Column(length = 50)
    private String userId;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 512)
    private String refreshToken;

    public static User createGuest() { ... }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
```

### 클래스 레벨

- `@Entity` — 이 클래스가 JPA가 관리하는 엔티티라는 표시. 이게 있어야 DB 테이블과 매핑되고 Repository에서 사용 가능
- `@Table(name = "users")` — 매핑할 테이블 이름 지정. 생략하면 클래스 이름(`user`)을 테이블명으로 쓰는데, `user`는 MySQL 등 여러 DB에서 **예약어**라 충돌이 날 수 있어서 `users`로 지정
- `extends BaseTimeEntity` — `createdAt`, `updatedAt` 컬럼을 상속받음 (`@MappedSuperclass` 덕분)

### 필드 레벨

#### @Id
- 기본키(PK) 지정. 엔티티에는 반드시 하나 있어야 함
- `@GeneratedValue`가 없으므로 **직접 값을 넣어줘야 함** (여기선 `"GUEST_xxxxxxxx"`). DB가 자동 증가시키는 방식이 아님

```java
// 자동 생성 방식 (참고) — DB의 AUTO_INCREMENT 사용
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

#### @Column
필드를 컬럼에 매핑하고 제약 조건을 설정한다. 생략해도 필드는 컬럼으로 매핑된다 (기본값 적용).

| 속성 | 의미 | 기본값 |
|------|------|--------|
| `name` | 컬럼 이름 | 필드명 (`userId` → `user_id`로 자동 변환) |
| `length` | 문자열 길이 (`VARCHAR(n)`) | 255 |
| `nullable` | `false`면 `NOT NULL` | `true` |
| `unique` | `true`면 `UNIQUE` 제약 | `false` |
| `updatable` | `false`면 UPDATE 쿼리에서 제외 | `true` |

- `password`는 `length`를 안 줬으므로 `VARCHAR(255)` — 암호화(BCrypt 등)된 값이 들어가므로 넉넉하게 기본값 사용
- `refreshToken`은 `nullable` 생략 → NULL 허용. 로그인 전에는 토큰이 없기 때문
- 카멜케이스 → 스네이크케이스 변환(`refreshToken` → `refresh_token`)은 Spring Boot 기본 네이밍 전략이 해줌

> `nullable`, `unique`, `length`는 `ddl-auto`로 테이블을 **자동 생성할 때** DDL에 반영되는 정보다. 이미 만들어진 테이블(`ddl.sql`)을 쓰는 경우 실제 제약은 DB가 걸고, 어노테이션은 문서 역할 + DDL과 맞춰두는 용도다.

#### @Enumerated
enum 타입을 DB에 어떻게 저장할지 지정한다.

| 옵션 | 저장 값 | 비고 |
|------|---------|------|
| `EnumType.STRING` | `"GUEST"`, `"USER"` (이름) | **권장** |
| `EnumType.ORDINAL` | `0`, `1` (순서) | 기본값. 위험 |

`ORDINAL`은 enum 중간에 상수를 추가하거나 순서를 바꾸면 기존 데이터의 의미가 바뀐다. 그래서 항상 `STRING`을 쓴다.

```java
enum Role { GUEST, USER }         // GUEST=0, USER=1
enum Role { GUEST, ADMIN, USER }  // ADMIN 추가 → 기존 1(USER)이 ADMIN이 됨 ❌
```

### 기본 생성자

JPA는 DB에서 조회한 데이터로 엔티티를 만들 때 **기본 생성자(파라미터 없는 생성자)로 객체를 만든 뒤** 필드에 값을 채운다. 그래서 엔티티에는 기본 생성자가 반드시 있어야 한다.

현재 코드는 생성자를 하나도 안 만들었으므로 Java가 `public User() {}`를 자동으로 만들어준다. 다만 `public`이면 외부에서 `new User()`로 빈 객체를 만들 수 있으므로, 보통 `protected`로 막는다.

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // Lombok
public class User extends BaseTimeEntity { ... }
```

`private`이 아니라 `protected`인 이유: JPA가 지연 로딩용 프록시 객체를 만들 때 엔티티를 상속하므로 `private`이면 프록시 생성이 안 된다.

### 정적 팩토리 메서드 (createGuest)

```java
public static User createGuest() {
    User user = new User();
    user.userId = "GUEST_" + UUID.randomUUID().toString().substring(0, 8);
    ...
    return user;
}
```

- 생성자 대신 `static` 메서드로 객체를 만드는 패턴. 이름(`createGuest`)으로 의도가 드러남
- 같은 클래스 내부라 `private` 필드에 직접 접근 가능 (`user.userId = ...`)
- 게스트 생성 규칙(ID 형식, 임시 비밀번호, `Role.GUEST`)을 엔티티 안에 모아둬서 서비스마다 중복으로 작성할 필요가 없음

### setter 대신 의도가 드러나는 메서드

```java
public void updateRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
}
```

- 엔티티에 `@Setter`를 열어두면 어디서든 아무 값이나 바꿀 수 있어서 추적이 어렵다
- 바꿔야 하는 값만 `updateXxx()` 같은 메서드로 열어둔다

### 변경 감지 (Dirty Checking)

트랜잭션 안에서 조회한 엔티티의 값을 바꾸면 **`save()`를 안 불러도** 트랜잭션이 끝날 때 UPDATE 쿼리가 자동으로 나간다.

```java
@Transactional
public void login(String userId, String token) {
    User user = userRepository.findById(userId).orElseThrow(...);
    user.updateRefreshToken(token);  // 값만 바꿈
}   // 트랜잭션 커밋 시 → UPDATE users SET refresh_token = ? ... 자동 실행
```

JPA가 조회 시점의 스냅샷과 현재 값을 비교해서 바뀐 게 있으면 UPDATE한다. 이때 `BaseTimeEntity`의 `@LastModifiedDate`도 함께 갱신된다.

### 참고: 직접 할당한 ID와 save()

`@GeneratedValue` 없이 ID를 직접 넣으면, `save()` 시점에 ID가 이미 있으므로 JPA는 "새 엔티티인지" 확인하려고 **SELECT를 먼저 한 번** 실행한 뒤 INSERT한다 (`persist`가 아닌 `merge` 동작). 기능상 문제는 없지만 쿼리가 하나 더 나간다는 점은 알아두자.

---

## JwtProperties (@ConfigurationProperties)

설정 파일(`application-local.properties`)의 값을 자바 객체로 묶어서 받는 기능이다. `@Value("${jwt.secret}")`를 필드마다 쓰는 대신, 관련 설정을 하나의 클래스로 모아서 타입 안전하게 사용한다.

### 코드

```properties
# application-local.properties (secret이 있으므로 커밋 X)
jwt.secret=...
jwt.access-token-expiration=1800000
jwt.refresh-token-expiration=604800000
jwt.issuer=cafe-reservation
```

```java
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration,
        String issuer
) {
}
```

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class CafeReservationApplication { ... }
```

- `prefix = "jwt"` — `jwt.`으로 시작하는 설정만 읽음. 없으면 어떤 값을 매핑할지 모름
- 케밥 케이스 → 카멜 케이스 자동 변환 (`access-token-expiration` → `accessTokenExpiration`)
- 만료 시간은 `long` — 숫자 계산에 바로 쓰고, ms 단위라 `int` 범위를 넘을 수 있음
- 사용: `jwtProperties.secret()` (record getter는 `get` 없이 필드명)

### 처음 작성했을 때의 실수

```java
@Component
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties() {        // 괄호가 비어 있음 → 필드 0개
    private static String secret;      // body의 static 필드 → 바인딩 대상 아님
}
```

- record는 **괄호 안에 선언한 것만 필드**가 된다. body `{}`에는 인스턴스 필드를 선언할 수 없음
- `static` 필드는 객체가 아니라 클래스에 속하므로, Spring이 객체에 값을 넣을 때 무시된다 → 항상 `null`

### 왜 @Component로 안 되는가?

Spring이 빈을 만드는 방식이 두 가지이고, record는 그중 하나로만 만들 수 있다.

#### @Component — 먼저 만들고, 나중에 값을 채움

```
1. Spring이 생성자 호출해서 객체 생성
   - 생성자 파라미터가 있으면 → 그 타입의 "빈"을 찾아서 주입 (의존성 주입)
2. 생성 후 → @ConfigurationProperties 처리기가 setter로 설정값 주입
```

record의 생성자는 `JwtProperties(String secret, long ...)` 하나뿐이다. 1단계에서 Spring은 "`String` 타입 빈이 있나?" 하고 찾는데, `String`은 빈이 아니라 값이므로 **실패**한다. record는 setter도 없어서 2단계 방식도 불가능하다.

```
JwtProperties is annotated with @ConstructorBinding but it is defined as a regular bean
which caused dependency injection to fail.
```

#### @ConfigurationPropertiesScan — 값을 먼저 읽고, 그 값으로 생성

```
1. 설정 파일에서 jwt.* 값을 먼저 읽음
2. 읽은 값을 생성자 파라미터에 넣어서 객체 생성
   new JwtProperties("exjP...", 1800000, 604800000, "cafe-reservation")
```

이 방식을 **생성자 바인딩(Constructor Binding)**이라고 한다. record처럼 생성자로만 값을 받는 객체는 이 방식으로만 만들 수 있다.

`@ConfigurationPropertiesScan` 대신 `@EnableConfigurationProperties(JwtProperties.class)`로 하나씩 등록해도 된다.

#### 한 줄 정리

> `@Component`는 객체부터 만들려고 생성자를 호출한다. 이때 생성자 파라미터(`String secret`, `long ...`)에 넣을 값이 **그 순간** 있어야 하는데, Spring은 그 값을 **빈 목록에서** 찾는다. `String`, `long` 타입 빈은 없으므로 오류가 난다.

- 값이 "아직 안 만들어진" 게 아니라 **찾는 곳이 틀린 것**이다. 설정값은 설정 파일에 있는데, 일반 빈 생성 과정은 설정 파일이 아니라 빈 목록만 본다
- "빈에서 찾는다" = 설정값이 아니라 **Spring 컨테이너에 등록된 객체**(Service, Repository 등)를 찾는다는 뜻
- 일반 클래스라면 기본 생성자로 파라미터 없이 먼저 만들고, 값은 나중에 setter로 채울 수 있다. record는 기본 생성자도 setter도 없어서 이 길이 막혀 있다

#### 참고: 생성자 주입과의 관계

파라미터 타입이 **빈으로 등록된 타입**이면 `@Component`로도 문제없이 생성된다. 이게 Service에서 쓰는 **생성자 주입**이다.

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;  // 빈 O → 주입 성공
}

@Component
public record JwtProperties(String secret, ...) {}  // String 빈 X → 주입 실패
```

같은 "생성자 호출" 과정이지만, 파라미터가 **빈**이면 성공하고 **설정값**이면 실패한다.

### 비교

| | `@Component` + 일반 클래스 | `@ConfigurationPropertiesScan` + record |
|---|---|---|
| 생성 | 기본 생성자로 빈 객체 생성 | 설정값으로 생성자 호출 |
| 값 주입 | 생성 후 **setter**로 | 생성 시 **생성자**로 |
| 필요한 것 | 기본 생성자 + setter (`@Getter @Setter`) | 없음 |
| 불변 | X (누구나 setter로 변경 가능) | O |

`@Component`를 쓰려면 `@Getter @Setter`를 붙인 일반 클래스로 만들어야 한다. 하지만 secret 같은 설정값이 실행 중에 바뀌면 안 되므로 **record + 생성자 바인딩**이 더 안전하다. 응답 DTO를 불변으로 만드는 이유와 같은 논리다.

---

## JwtTokenProvider에서 쓴 문법

### `jwtProperties.secret()` — record 접근자 메서드

record는 괄호 안에 선언한 필드마다 **필드명과 같은 이름의 메서드**를 자동으로 만들어준다. 이를 접근자(accessor) 메서드라고 한다.

> **즉, record의 getter다.** 필드 값을 꺼내주는 역할은 getter와 완전히 같고, 이름 규칙만 다르다 (`getSecret()` → `secret()`). "record의 getter"라고 불러도 뜻은 통하지만, 호출할 때 `get`을 붙이면 그런 메서드가 없어서 컴파일 에러가 난다.

```java
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration,
        String issuer
) {}
```

위 코드를 쓰면 컴파일러가 아래 메서드를 자동으로 만든다.

```java
public String secret() { return secret; }
public long accessTokenExpiration() { return accessTokenExpiration; }
public long refreshTokenExpiration() { return refreshTokenExpiration; }
public String issuer() { return issuer; }
```

그래서 직접 작성하지 않아도 이렇게 쓸 수 있다.

```java
jwtProperties.secret()                  // "exjP..."
jwtProperties.accessTokenExpiration()   // 1800000
```

#### 일반 클래스 getter와의 차이

| | record | 일반 클래스 + `@Getter` |
|---|---|---|
| 메서드 이름 | `secret()` | `getSecret()` |
| 만드는 주체 | Java 컴파일러 (내장) | Lombok |
| setter | 없음 | `@Setter` 붙이면 생김 |

- record는 `get` 접두사가 **없다**. `jwtProperties.getSecret()`이라고 쓰면 컴파일 에러
- 필드는 `private final`이라 `jwtProperties.secret`처럼 필드에 직접 접근은 불가능하고, 반드시 메서드로 꺼낸다

### `role.name()` — enum 상수 이름을 문자열로

모든 enum은 `java.lang.Enum`을 자동으로 상속받고, `name()` 메서드는 **선언한 상수 이름 그대로**를 `String`으로 반환한다.

```java
public enum Role {
    USER, ADMIN, GUEST
}

Role role = Role.GUEST;
role.name()   // "GUEST" (String)
```

| `role` 값 | `role.name()` 결과 |
|---|---|
| `Role.USER` | `"USER"` |
| `Role.ADMIN` | `"ADMIN"` |
| `Role.GUEST` | `"GUEST"` |

토큰 생성에 적용하면:

```java
createAccessToken("GUEST_a1b2c3d4", Role.GUEST);

.claim("role", role.name())   // → .claim("role", "GUEST")
                              // → payload: { "role": "GUEST" }
```

> **바뀌는 건 타입이다.** `role`을 출력하든 `role.name()`을 출력하든 화면엔 똑같이 `GUEST`로 보이지만, `role`은 **`Role` 타입의 enum 객체**이고 `role.name()`은 **`String` 타입의 문자열**이다.

반대로 문자열을 enum으로 바꿀 때는 `valueOf()`를 쓴다.

```java
Role.valueOf("GUEST")   // Role.GUEST
Role.valueOf("guest")   // IllegalArgumentException (대소문자까지 정확히 일치해야 함)
Role.valueOf(null)      // NullPointerException: Name is null
```

#### `valueOf()` 입력별 결과

| 넣은 값 | 결과 |
|---|---|
| 정확한 이름 (`"GUEST"`) | `Role.GUEST` |
| 없는 이름 (`"guest"`, `"ABC"`) | `IllegalArgumentException` (`No enum constant ...Role.guest`) |
| `null` | **`NullPointerException`** (`Name is null`) |

> **`Role.valueOf(null)`은 `null`을 돌려주지 않고 바로 NPE를 던진다.** 그래서 `valueOf` 결과를 받은 뒤에 null 체크를 해도 소용없고, **`valueOf`를 부르기 전에** null을 걸러야 한다.

`Role.valueOf(String)`은 컴파일러가 enum마다 자동으로 만들어주는 메서드다. 내부에서 `Enum.valueOf(Role.class, name)`을 호출하는데, 이 메서드가 가장 먼저 `name`이 null인지 검사해서 `NullPointerException("Name is null")`을 던진다.

#### 왜 토큰에 `role` 대신 `role.name()`을 넣는가?

JWT의 payload는 결국 **JSON 문자열**이다. 토큰에는 Java 객체가 아니라 문자열/숫자 같은 단순한 값이 들어가야 한다.

```java
.claim("role", role.name())   // payload: { "role": "GUEST" }
```

- **넣을 때**: 문자열로 명확하게 저장 → enum을 JSON으로 어떻게 변환할지 라이브러리에 맡기지 않아도 됨
- **꺼낼 때**: 토큰에서 꺼내면 어차피 `String`이므로, `Role.valueOf()`로 다시 enum으로 바꾼다

```java
// 생성
.claim("role", role.name())                       // Role → String

// 파싱
String roleName = claims.get("role", String.class);
Role role = Role.valueOf(roleName);                // String → Role
```

#### `name()` vs `toString()`

둘 다 기본적으로 `"GUEST"`를 반환하지만, `toString()`은 enum에서 **재정의(override)할 수 있어서** 다른 값이 나올 수 있다. `name()`은 `final`이라 재정의가 불가능하고 항상 상수 이름을 반환한다. 그래서 저장/변환용으로는 `name()`을 쓴다.

```java
public enum Role {
    GUEST;
    @Override
    public String toString() { return "비회원"; }
}

Role.GUEST.toString()  // "비회원"
Role.GUEST.name()      // "GUEST" ← 항상 같음
```

> `@Enumerated(EnumType.STRING)`이 DB에 `"GUEST"`를 저장하는 것도 내부적으로 `name()`을 사용한다.

### 토큰 생성 코드 (`Jwts.builder()`)

```java
public String createAccessToken(String userId, Role role) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtProperties.accessTokenExpiration());

    return Jwts.builder()
            .subject(userId)
            .claim("role", role.name())
            .issuer(jwtProperties.issuer())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
}
```

#### 빌더 패턴 + 메서드 체이닝

- `Jwts.builder()` — 토큰을 조립할 **빌더 객체**를 만든다
- `.subject(...)`, `.claim(...)` 등 — 각 메서드가 값을 설정한 뒤 **빌더 자신을 다시 반환**하기 때문에 `.`으로 계속 이어 붙일 수 있다 (메서드 체이닝)
- `.compact()` — 마지막에 조립을 끝내고 결과물(토큰 문자열)을 반환한다

```java
// 체이닝 없이 쓰면 이런 모양 (같은 의미)
JwtBuilder builder = Jwts.builder();
builder = builder.subject(userId);
builder = builder.claim("role", role.name());
...
String token = builder.compact();
```

값이 많은 객체를 만들 때 생성자 파라미터를 길게 나열하는 대신, **어떤 값을 넣는지 이름이 보이게** 조립할 수 있어서 쓴다.

#### 각 메서드의 의미

| 메서드 | payload 키 | 의미 | 값 |
|---|---|---|---|
| `.subject(userId)` | `sub` | 토큰의 주인 (누구의 토큰인가) | `"GUEST_a1b2c3d4"` |
| `.claim("role", role.name())` | `role` | 직접 정의한 커스텀 값 | `"GUEST"` |
| `.issuer(...)` | `iss` | 토큰 발급자 | `"cafe-reservation"` |
| `.issuedAt(now)` | `iat` | 발급 시각 | 1759300000 |
| `.expiration(expiryDate)` | `exp` | 만료 시각 — 지나면 파싱 시 `ExpiredJwtException` | 1759301800 |
| `.signWith(key)` | (header의 `alg`) | 비밀 키로 서명 | |
| `.compact()` | | 토큰 문자열로 변환 | `"eyJhbGc..."` |

- `sub`, `iss`, `iat`, `exp`는 JWT 표준(RFC 7519)에 정해진 **등록된 클레임**이라 전용 메서드가 있다
- `role`처럼 표준에 없는 값은 `.claim(키, 값)`으로 넣는다
- `iat`, `exp`는 토큰 안에 초 단위 숫자로 저장된다 (`Date` → 자동 변환)

#### 만료 시각 계산

```java
Date now = new Date();                                   // 현재 시각
Date expiryDate = new Date(now.getTime() + 1800000);     // 현재 + 30분
```

- `now.getTime()` — `Date`를 1970-01-01부터 지난 **밀리초(long)**로 변환
- 여기에 만료 시간(ms)을 더해서 다시 `Date`로 만든다
- `now`를 한 번만 구해서 `issuedAt`과 `expiration`에 같이 써야 두 시각의 기준이 정확히 같다

#### 서명 키 (`getSigningKey`)

```java
private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
}
```

1. `jwtProperties.secret()` — 설정 파일의 Base64 문자열
2. `Decoders.BASE64.decode(...)` — Base64 문자열 → 원본 바이트 배열 (`byte[]`)
3. `Keys.hmacShaKeyFor(...)` — 바이트 배열 → HMAC-SHA 서명용 `SecretKey`

`signWith(key)`는 키 길이를 보고 알고리즘을 자동 선택한다 (32바이트 이상 → HS256, 48바이트 이상 → HS384, 64바이트 이상 → HS512). 키가 32바이트보다 짧으면 `WeakKeyException`이 발생한다.

#### `compact()` 결과물

```
eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJHVUVTVF9hMWIyYzNkNCIsInJvbGUiOiJHVUVTVCIsLi4ufQ.xxxxxxxx
└────── header ──────┘ └────────────────── payload ──────────────────────────┘ └ 서명 ┘
```

`.`으로 구분된 세 부분이며, 각각 Base64URL로 인코딩되어 있다.

| 부분 | 내용 |
|---|---|
| header | 서명 알고리즘 `{"alg":"HS384"}` |
| payload | 넣은 값들 `{"sub":"GUEST_a1b2c3d4","role":"GUEST","iss":"cafe-reservation","iat":...,"exp":...}` |
| signature | header + payload를 비밀 키로 서명한 값 |

> payload는 **암호화가 아니라 인코딩**이라 누구나 디코딩해서 읽을 수 있다 (jwt.io에 붙여넣으면 바로 보임). 그래서 비밀번호 같은 민감 정보는 절대 넣으면 안 된다. 서명은 "내용이 위조되지 않았음"을 보장할 뿐, 내용을 숨기지 않는다.

### 토큰 파싱 코드 (`Jwts.parser()`)

```java
private Claims parseClaims(String token) {
    return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
}
```

토큰 생성(`builder`)의 **반대 과정**이다. 토큰 문자열을 받아서 서명을 검증하고, payload 값들을 꺼낸다.

#### 각 메서드의 의미

| 메서드 | 반환 타입 | 의미 |
|---|---|---|
| `Jwts.parser()` | `JwtParserBuilder` | 파서를 설정할 **빌더** 생성 |
| `.verifyWith(key)` | `JwtParserBuilder` | 서명 검증에 쓸 키 지정 (생성 때 `signWith`에 쓴 것과 **같은 키**) |
| `.build()` | `JwtParser` | 설정을 끝내고 실제 파서 객체 생성 |
| `.parseSignedClaims(token)` | `Jws<Claims>` | 토큰 파싱 + 서명 검증 + 만료 검증. **실패하면 예외** |
| `.getPayload()` | `Claims` | 검증된 payload(값 묶음) 꺼내기 |

#### 왜 `build()`가 중간에 있는가?

생성 쪽(`Jwts.builder()`)은 `compact()`로 바로 끝나는데, 파싱 쪽은 `build()`가 한 번 더 있다.

```
Jwts.parser()              ← 파서 "설정"을 위한 빌더
    .verifyWith(key)       ← 설정: 이 키로 검증해라
    .build()               ← 설정 완료 → 파서 객체 완성
    .parseSignedClaims()   ← 완성된 파서로 실제 파싱
```

**파서를 만드는 단계**와 **파서를 사용하는 단계**가 나뉘어 있다. 설정(키 등)이 끝난 파서는 여러 토큰에 재사용할 수 있다.

#### `parseSignedClaims`에서 하는 일

```
1. 토큰을 "." 기준으로 header / payload / signature로 분리
2. header + payload를 내 키로 다시 서명해서 → 토큰의 signature와 비교
   → 다르면 SignatureException (위조됨)
3. payload의 exp를 현재 시각과 비교
   → 지났으면 ExpiredJwtException (만료됨)
4. 모두 통과하면 Jws<Claims> 반환
```

**검증을 통과해야만 값을 꺼낼 수 있다.** 그래서 `getUserId`, `getRole`도 `parseClaims`를 거치는 순간 자동으로 서명/만료 검증이 같이 된다.

#### 발생하는 예외

| 예외 | 상황 |
|---|---|
| `ExpiredJwtException` | `exp`가 지남 |
| `SignatureException` | 서명 불일치 (다른 키로 만들었거나 내용이 변조됨) |
| `MalformedJwtException` | 토큰 형식이 아님 (`.`이 3부분이 아닌 등) |
| `UnsupportedJwtException` | 지원하지 않는 형식 (서명 없는 토큰 등) |
| `IllegalArgumentException` | 토큰이 `null`이거나 빈 문자열 |

`IllegalArgumentException`을 제외한 나머지는 모두 `JwtException`의 하위 클래스다. 그래서 `validateToken`에서는 두 개만 잡으면 충분하다.

```java
public boolean validateToken(String token) {
    try {
        parseClaims(token);
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        return false;
    }
}
```

`catch (A | B e)`는 **멀티 캐치** 문법으로, 두 예외를 같은 방식으로 처리할 때 한 블록으로 묶는다.

#### `Claims`에서 값 꺼내기

`Claims`는 payload를 담은 `Map` 형태의 객체다.

```java
Claims claims = parseClaims(token);

claims.getSubject()                 // "GUEST_a1b2c3d4"   (sub — 표준 클레임은 전용 메서드)
claims.getIssuer()                  // "cafe-reservation" (iss)
claims.getExpiration()              // Date               (exp)
claims.get("role", String.class)    // "GUEST"            (커스텀 클레임은 get(키, 타입))
```

- 표준 클레임(`sub`, `iss`, `iat`, `exp`)은 `getSubject()` 같은 전용 메서드가 있다
- 커스텀 클레임은 `get(키, 타입)`으로 꺼낸다. 두 번째 인자로 타입을 지정해서 형변환까지 해준다

#### `parseClaims(token).get("role", String.class)` 뜯어보기

```java
parseClaims(token).get("role", String.class)
└──── ① ────────┘ └──────── ② ─────────────┘
```

**① `parseClaims(token)` — 메서드 반환값에 바로 이어서 호출**

`parseClaims(token)`의 반환 타입이 `Claims`이므로, 그 결과에 바로 `.get(...)`을 붙일 수 있다. 아래 두 코드는 같다.

```java
// 한 줄로
String roleName = parseClaims(token).get("role", String.class);

// 풀어서
Claims claims = parseClaims(token);
String roleName = claims.get("role", String.class);
```

**② `.get("role", String.class)` — 키로 값 꺼내기 + 타입 지정**

| 인자 | 의미 |
|---|---|
| `"role"` | 꺼낼 값의 키 (생성할 때 `.claim("role", ...)`에 쓴 이름과 같아야 함) |
| `String.class` | 꺼낸 값을 **어떤 타입으로 받을지** 지정 |

#### `String.class`란?

`클래스명.class`는 그 클래스의 **타입 정보 자체**를 값으로 넘기는 문법이다 (`Class` 타입 객체). "String이라는 타입"을 메서드에 인자로 전달하는 것이다.

```java
String.class    // Class<String>  — "String 타입"이라는 정보
Integer.class   // Class<Integer> — "Integer 타입"이라는 정보
```

`get` 메서드는 제네릭으로 선언되어 있어서, 넘긴 타입이 그대로 **반환 타입**이 된다.

```java
<T> T get(String claimName, Class<T> requiredType);
// String.class 를 넘기면 → T = String → 반환 타입 String
```

#### 왜 타입을 넘기는가? — `get("role")`만 쓰면?

`Claims`는 `Map<String, Object>`를 상속하므로 `get("role")`만 쓸 수도 있다. 하지만 반환 타입이 `Object`라 직접 형변환해야 한다.

```java
// 타입 지정 X — Object로 나와서 직접 캐스팅 필요
String roleName = (String) claims.get("role");

// 타입 지정 O — 바로 String으로 나옴
String roleName = claims.get("role", String.class);
```

- payload에는 문자열, 숫자, 날짜 등 **여러 타입의 값이 섞여** 있어서, 기본 `get`은 모든 타입을 담을 수 있는 `Object`를 반환한다
- `String.class`를 넘기면 jjwt가 "이 값은 String이어야 한다"고 확인하고 변환해서 돌려준다
- 실제 값이 지정한 타입과 맞지 않으면 `RequiredTypeException`이 발생한다 → 잘못된 캐스팅을 조용히 넘기지 않음

#### 최종: String → Role

토큰에서 꺼낸 값은 `String`이므로, `Role.valueOf()`로 다시 enum으로 바꾼다.

```java
public Role getRole(String token) {
    String role = parseClaims(token).get("role", String.class);  // AT: "GUEST" / RT: null
    return role != null ? Role.valueOf(role) : null;              // null이면 valueOf를 아예 안 부름
}
```

- `조건 ? A : B` — 삼항 연산자. 조건이 참이면 A, 거짓이면 B (if/else와 같음)

#### 처음 작성했을 때의 실수 — Refresh Token을 넣으면 500 에러

처음 코드는 null 처리가 없었다.

```java
public Role getRole(String token) {
    return Role.valueOf(parseClaims(token).get("role", String.class));
}
```

- Access Token(AT)에는 `role` 클레임이 있지만, **Refresh Token(RT)에는 없다** (`createRefreshToken`에서 `.claim("role", ...)`을 안 넣음)
- RT도 같은 키로 서명했으므로 `validateToken`은 **통과**한다
- 그래서 클라이언트가 `Authorization` 헤더에 RT를 넣으면 → `get("role", ...)`이 `null` → `Role.valueOf(null)` → **NPE → 500 에러**

```
Authorization: Bearer {RT}
→ validateToken(RT)    → true (서명 OK)
→ getRole(RT)          → Role.valueOf(null) → 💥 NullPointerException
```

**처음 시도한 해결 — Filter에서 null 체크 (효과 없음)**

```java
// Filter
Role role = jwtTokenProvider.getRole(token);   // ← 이 줄 안에서 이미 NPE
if (role != null) { ... }                       // ← 여기까지 못 옴
```

`valueOf`가 null을 반환하는 게 아니라 **예외를 던지기** 때문에, 호출한 쪽에서 null 체크를 해도 의미가 없다.

**최종 해결 — Provider에서 `valueOf` 전에 null 체크 + Filter에서 role 있는 경우만 인증**

```java
// Provider: RT면 null 반환
return role != null ? Role.valueOf(role) : null;

// Filter: role이 있는 토큰(= AT)만 인증 등록
if (role != null) {
    // 인증 객체 생성 + SecurityContext 저장
}
```

> 더 정석적인 방법은 토큰 생성 시 `.claim("type", "access")` / `.claim("type", "refresh")`를 넣고, Filter에서 access 타입만 받는 것이다. RT로 API를 호출하는 것 자체를 명시적으로 막을 수 있다.

#### 생성 ↔ 파싱 대응

| 생성 (`builder`) | 파싱 (`parser`) |
|---|---|
| `.signWith(key)` | `.verifyWith(key)` |
| `.subject(userId)` | `claims.getSubject()` |
| `.claim("role", role.name())` | `Role.valueOf(claims.get("role", String.class))` |
| `.expiration(date)` | 자동 검증 (지나면 `ExpiredJwtException`) |
| `.compact()` → 문자열 | `.parseSignedClaims(문자열)` |

---

## 추상 클래스 상속과 구현 필수 메서드

추상 클래스를 상속하면 **추상 메서드만** 반드시 구현해야 한다. 이미 몸통 `{}`이 있는 일반 메서드는 그대로 물려받고, 필요할 때만 재정의한다.

### 코드 (JwtAuthenticationFilter)

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 필터 로직
    }
}
```

`OncePerRequestFilter`의 추상 메서드는 `doFilterInternal` 하나뿐이라, 이것만 구현하면 된다. 구현하지 않으면 컴파일 에러가 난다.

> IntelliJ: `extends OncePerRequestFilter`까지 쓰고 클래스명에서 `⌥ + Enter` → **Implement methods** (또는 `⌃ + I`)를 누르면 자동 생성된다.

### 추상 메서드 vs 일반 메서드

```java
public abstract class OncePerRequestFilter {

    // 일반 메서드 — 이미 구현돼 있음 → 그대로 물려받음
    public final void doFilter(...) {
        // "이 요청에서 이미 실행했나?" 확인
        // 처음이면 → doFilterInternal(...) 호출
    }

    // 추상 메서드 — 몸통이 없음 → 자식이 반드시 채워야 함
    protected abstract void doFilterInternal(...);
}
```

| 메서드 종류 | 구현 필수? | 예시 (`OncePerRequestFilter`) |
|---|---|---|
| 추상 메서드 (`abstract`, 몸통 `{}` 없음) | O 필수 | `doFilterInternal` |
| 일반 메서드 (몸통 `{}` 있음) | X 선택 (필요하면 `@Override`) | `shouldNotFilter` 등 |
| `final` 메서드 | 재정의 **불가** | `doFilter` |

> **"추상 클래스를 상속하면 메서드 구현이 필수인가?" → 추상 메서드만 필수다.** 추상 클래스의 모든 메서드를 구현해야 하는 게 아니다.

### 예외: 자식도 추상 클래스면

자식 클래스도 `abstract`로 선언하면 구현하지 않아도 된다. 구현 의무가 그다음 자식에게 넘어간다.

```java
public abstract class MyFilter extends OncePerRequestFilter { }            // OK — 구현 안 해도 됨
public class RealFilter extends MyFilter { /* doFilterInternal 구현 */ }    // 여기서 필수
```

### 왜 `doFilter`가 아니라 `doFilterInternal`인가? — 템플릿 메서드 패턴

필터의 원래 진입점은 `doFilter`지만, `OncePerRequestFilter`가 이미 구현해두었다 (`final`이라 바꿀 수도 없음).

```
요청 → doFilter (부모가 구현, 공통 로직)
         → 이 요청에서 이미 실행했나?
             → 했으면 → 건너뜀
             → 처음이면 → doFilterInternal 호출 (자식이 구현, 실제 로직)
```

- **공통 흐름**(요청당 한 번만 실행)은 부모가 가지고 있고
- **자식마다 달라지는 부분**(실제 필터 내용)만 추상 메서드로 비워둔다

이런 설계를 **템플릿 메서드 패턴**이라고 한다. 이름의 **Once Per Request**(요청당 한 번)가 부모가 보장해주는 공통 로직이다.

### 추상 클래스 vs 인터페이스

| | 추상 클래스 | 인터페이스 |
|---|---|---|
| 구현 필수 | 추상 메서드만 | 모든 메서드 (`default` 메서드 제외) |
| 미리 구현된 로직 | 가질 수 있음 (공통 로직) | `default` 메서드로만 |
| 필드 | 가질 수 있음 | 상수(`static final`)만 |
| 상속 | `extends` — 하나만 | `implements` — 여러 개 가능 |

`BaseTimeEntity`도 추상 클래스지만 추상 메서드가 없다. 이 경우 `abstract`는 "구현 강제"가 아니라 **`new BaseTimeEntity()`로 직접 생성하는 것을 막는** 용도다. (→ "JpaConfig (@EnableJpaAuditing)" 섹션 참고)

---

## JwtAuthenticationFilter

요청이 올 때마다 헤더의 토큰을 꺼내 검증하고, 통과하면 **"이 요청은 이 사용자의 것"이라고 Spring Security에 등록**하는 필터다.

### Filter와 Provider의 역할 구분

"Filter = 토큰 검증 클래스"는 반만 맞다. **검증 자체는 Provider**가 하고, Filter는 그 검증을 **요청마다 호출해서 결과를 SecurityContext에 등록**한다.

| 클래스 | 다루는 대상 | 역할 |
|---|---|---|
| `JwtTokenProvider` | 토큰 | 생성, 서명/만료 검증, 값 꺼내기 |
| `JwtAuthenticationFilter` | 요청 | 헤더에서 토큰 추출 → Provider에 검증 위임 → 인증 등록 |

```
요청 → [JwtAuthenticationFilter] ─────────────────────────→ Controller
         1. 헤더에서 토큰 꺼내기          (Filter — resolveToken)
         2. validateToken(토큰)         → Provider
         3. getUserId, getRole          → Provider
         4. SecurityContext에 인증 등록   (Filter)
```

> Filter는 토큰이 없거나 틀려도 **401을 직접 응답하지 않는다.** 등록 없이 그냥 통과시키고, 이 URL이 인증이 필요한지(→ 401)는 SecurityConfig가 판단한다. `permitAll` 경로는 토큰 없이도 통과해야 하기 때문이다.

### 코드

```java
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String userId = jwtTokenProvider.getUserId(token);
            Role role = jwtTokenProvider.getRole(token);

            if (role != null) {   // role이 있는 토큰(= AT)만 인증
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);   // ← 무조건 실행 (if 밖)
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- `extends OncePerRequestFilter` — 요청당 한 번만 실행되는 필터. `doFilterInternal`만 구현하면 됨 (→ "추상 클래스 상속과 구현 필수 메서드" 섹션)
- `@Component`를 붙이지 않는다 — 붙이면 Spring Boot가 일반 서블릿 필터로도 자동 등록해서 **두 번 실행**될 수 있음. SecurityConfig에서 `new JwtAuthenticationFilter(jwtTokenProvider)`로 직접 등록
- `filterChain.doFilter(request, response)` — 다음 필터로 넘김. 빠지거나 `if` 안에 있으면 토큰 없는 요청이 Controller까지 가지 못한다

### 전체 코드 한 줄씩 보기

#### 클래스 선언부

```java
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
```

- `@RequiredArgsConstructor` — `final` 필드(`jwtTokenProvider`)를 받는 생성자를 Lombok이 만들어줌 → `new JwtAuthenticationFilter(jwtTokenProvider)`로 생성
- `extends OncePerRequestFilter` — 한 요청에서 **딱 한 번만** 실행되는 필터의 부모 클래스
- `private final JwtTokenProvider` — 토큰 검증/값 꺼내기를 맡길 Provider. Filter는 토큰을 직접 파싱하지 않고 Provider에게 위임한다
- `@Component` 없음 — 빈으로 자동 등록하지 않고 SecurityConfig에서 직접 생성해서 등록

#### 메서드 시그니처

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
```

| 요소 | 의미 |
|---|---|
| `@Override` | 부모의 추상 메서드를 구현한다는 표시 |
| `protected` | 부모에 `protected`로 선언돼 있어서 그대로 맞춤 (더 좁힐 수 없음) |
| `HttpServletRequest request` | 들어온 요청 — 헤더, URL, 파라미터 등을 꺼낼 수 있음 |
| `HttpServletResponse response` | 나갈 응답 — 여기선 직접 쓰지 않고 다음 필터에 그대로 전달 |
| `FilterChain filterChain` | 다음 필터들로 이어지는 사슬. `doFilter`를 호출해야 다음으로 넘어감 |
| `throws ServletException, IOException` | `filterChain.doFilter`가 던질 수 있는 예외. 부모 시그니처와 동일 |

#### 본문 흐름

```java
// 1. 헤더에서 토큰 꺼내기
String token = resolveToken(request);
```
`Authorization: Bearer eyJ...` → `"eyJ..."`. 헤더가 없거나 `Bearer `로 시작하지 않으면 `null`.

```java
// 2. 토큰이 있고 유효하면
if (token != null && jwtTokenProvider.validateToken(token)) {
```
- `&&`는 **앞이 false면 뒤를 실행하지 않는다** (단축 평가). 그래서 `token`이 `null`이면 `validateToken`을 아예 호출하지 않음
- `validateToken` — 서명·만료 검증. 실패하면 `false` (예외를 던지지 않음)

```java
// 3. 토큰에서 userId, role 꺼내기
String userId = jwtTokenProvider.getUserId(token);   // sub
Role role = jwtTokenProvider.getRole(token);         // AT: Role.GUEST 등 / RT: null
if (role != null) {
```
role이 있는 토큰 = Access Token만 인증한다. Refresh Token은 role이 없어서 여기서 걸러진다.

```java
    // 4. 인증 객체 만들기
    UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );

    // 5. SecurityContext에 저장
    SecurityContextHolder.getContext().setAuthentication(authentication);
}
```
"이 요청은 `GUEST_a1b2c3d4`가 보냈고 권한은 `ROLE_GUEST`"라고 Spring Security에 등록한다. (각 부분은 아래 "인증 객체", "권한 목록" 참고)

```java
// 6. 다음 필터로 넘기기 (토큰이 없거나 틀려도 무조건 실행)
filterChain.doFilter(request, response);
```
인증 성공이든 실패든 **항상** 다음 필터로 넘긴다. 실패한 요청을 막는 건 뒤에 있는 Spring Security(SecurityConfig 설정)의 몫이다.

### 상황별 동작

| 요청 | `resolveToken` | `validateToken` | `role` | SecurityContext | 이후 결과 |
|---|---|---|---|---|---|
| 헤더 없음 | `null` | (호출 안 함) | — | 비어 있음 | permitAll → 통과 / 인증 필요 → 401 |
| `Authorization: abc` (Bearer 아님) | `null` | (호출 안 함) | — | 비어 있음 | 위와 같음 |
| 유효한 AT | 토큰 | `true` | `GUEST` 등 | **인증 등록** | 권한에 맞으면 Controller 도달 |
| 만료된 AT | 토큰 | `false` | — | 비어 있음 | 인증 필요 URL이면 401 |
| 위조된 토큰 | 토큰 | `false` | — | 비어 있음 | 인증 필요 URL이면 401 |
| RT를 넣음 | 토큰 | `true` | `null` | 비어 있음 | 인증 필요 URL이면 401 |

> **Filter는 "인증 정보를 채워주는" 역할만 한다.** 막을지 말지는 SecurityContext가 비어 있는지를 보고 Spring Security가 결정한다.

### 필터 체인에서의 위치

```
요청
 → ... (Spring Security 기본 필터들)
 → JwtAuthenticationFilter          ← 여기서 SecurityContext 채움
 → UsernamePasswordAuthenticationFilter (폼 로그인용 — 우리는 안 씀)
 → ...
 → AuthorizationFilter               ← SecurityContext를 보고 허용/401/403 결정
 → DispatcherServlet → Controller
```

SecurityConfig에서 `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`로 **인가 검사보다 앞에** 끼워 넣는다. 그래야 권한 검사 시점에 인증 정보가 이미 들어 있다.

> 필터에서 발생한 예외는 `GlobalExceptionHandler`(`@RestControllerAdvice`)가 잡지 못한다. 필터는 DispatcherServlet보다 **앞**에서 실행되기 때문이다. 그래서 Filter 안에서는 예외를 던지지 않고(`validateToken`이 `false` 반환), 401/403 응답 형식은 SecurityConfig의 `AuthenticationEntryPoint`/`AccessDeniedHandler`로 맞춘다.

### 처음 작성했을 때의 실수 모음

| 실수 | 증상 | 해결 |
|---|---|---|
| `startsWith("bearer")` | 토큰이 항상 인식 안 됨 | `startsWith("Bearer ")` |
| `getRole`에 null 처리 없음 | RT를 넣으면 NPE → 500 | `role != null ? Role.valueOf(role) : null` |
| `List.of(...)),` | `illegal start of expression` | 마지막 인자 뒤 쉼표 삭제 |
| `"ROLE" + role.name()` | 컴파일은 되지만 `hasRole` 불일치 → 403 | `"ROLE_" + role.name()` |
| `setAuthentication`이 `if` 블록 밖 | `authentication` 변수 범위 밖 → 컴파일 에러 | 같은 블록 안으로 이동 |

### resolveToken — 직접 만든 private 메서드

- 부모(`OncePerRequestFilter`)에 있는 메서드가 아니라 **직접 만든 메서드**다. 그래서 `@Override`가 없다 (붙이면 컴파일 에러)
- "헤더에서 Bearer 토큰 꺼내기"라는 세부 작업을 분리해서 `doFilterInternal`을 읽기 쉽게 한다 (Provider의 `parseClaims`와 같은 이유)

| 구분법 | `doFilterInternal` | `resolveToken` |
|---|---|---|
| 출처 | 부모의 추상 메서드 구현 | 직접 만든 메서드 |
| `@Override` | O | X |

#### 처음 작성했을 때의 실수 — `startsWith("bearer")`

```java
if (bearer != null && bearer.startsWith("bearer")) {   // ❌
```

- 클라이언트는 `Authorization: Bearer eyJhbGc...`로 보낸다
- `startsWith`는 **대소문자를 구분**하므로 `"Bearer ..."`는 `"bearer"`로 시작하지 않음 → **항상 false** → 모든 요청이 "토큰 없음"으로 처리됨
- 뒤의 **공백까지** 포함해야 정확하다. `substring(7)`도 `"Bearer "`가 공백 포함 7글자라는 전제

```java
if (bearer != null && bearer.startsWith("Bearer ")) {  // ✅
```

> 개선: `7`은 매직 넘버. `private static final String BEARER_PREFIX = "Bearer ";`로 빼고 `substring(BEARER_PREFIX.length())`로 쓰면 글자 수를 셀 필요가 없다.

### 인증 객체 — `UsernamePasswordAuthenticationToken`

```java
new UsernamePasswordAuthenticationToken(
        userId,                 // principal   — 누구인지 → "GUEST_a1b2c3d4"
        null,                   // credentials — 비밀번호 → 토큰으로 이미 검증했으니 null
        List.of(...)            // authorities — 권한 목록 → [ROLE_GUEST]
);
```

| 파라미터 | 의미 | 값 |
|---|---|---|
| principal | 인증된 사용자 (Controller에서 "누가 요청했는지" 꺼낼 때 나오는 값) | `userId` |
| credentials | 비밀번호 등 인증 수단 | `null` |
| authorities | 권한 목록 | `List.of(new SimpleGrantedAuthority(...))` |

#### 파라미터 3개를 넣어야 "인증 완료"

| 생성자 | 상태 |
|---|---|
| `(principal, credentials)` — 2개 | **인증 전** (`isAuthenticated() == false`) — 로그인 시도용 |
| `(principal, credentials, authorities)` — 3개 | **인증 완료** (`isAuthenticated() == true`) |

2개짜리로 만들어 SecurityContext에 넣으면 **인증되지 않은 사용자로 취급**된다. 권한 목록이 필요 없어 보여도 3개짜리를 써야 하는 이유다.

#### JWT vs `UsernamePasswordAuthenticationToken` — 둘 다 "Token"인데 뭐가 다른가?

이름에 둘 다 Token이 들어가지만 **완전히 다른 것**이다.

| | JWT (Provider가 만듦) | `UsernamePasswordAuthenticationToken` (Filter가 만듦) |
|---|---|---|
| 정체 | **문자열** `"eyJhbGc..."` | **Java 객체** (서버 메모리) |
| 사용 범위 | 클라이언트 ↔ 서버가 주고받음 | 서버 안에서 Spring Security만 사용 |
| 위치 | 클라이언트 localStorage, 요청 헤더 | `SecurityContextHolder` |
| 수명 | AT 30분, RT 7일 | **요청 하나가 끝나면 사라짐** |
| 만드는 시점 | 로그인할 때 한 번 | **요청마다** 매번 |
| 역할 | "나 이 사람이에요" — 증명서 | "이 요청은 이 사람이 보냄" — 서버 내부 확인 도장 |

비유: JWT는 놀이공원 **입장 팔찌**(입구에서 한 번 받아 계속 차고 다님), `UsernamePasswordAuthenticationToken`은 놀이기구마다 직원이 팔찌를 확인하고 찍어주는 **도장**(그 놀이기구 탈 때만 유효).

```
[로그인]
Provider.createAccessToken() → JWT 문자열 → 클라이언트 저장

[API 요청마다]
Authorization: Bearer {JWT}
→ Filter: JWT 검증 (Provider)
→ Filter: new UsernamePasswordAuthenticationToken(userId, null, 권한)   ← JWT 내용으로 생성
→ SecurityContext에 저장 → 권한 검사, Controller에서 사용
→ 응답 후 SecurityContext 비워짐 (객체 사라짐)
```

> **Filter는 JWT를 Spring Security가 이해하는 형식으로 번역한다.** Spring Security는 JWT를 직접 읽지 못하고 `Authentication` 타입 객체만 안다. `UsernamePasswordAuthenticationToken`은 `Authentication`의 구현체다.

**이름이 왜 UsernamePassword인가?** 원래 **아이디/비밀번호 폼 로그인**용으로 만든 클래스라서다. JWT 방식에서는 credentials 자리에 `null`을 넣고 "인증 완료" 상태를 표현하는 용도로 빌려 쓴다 (실무에서 가장 흔한 방식).

### 권한 목록 — `List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))`

```java
List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
│       │                          └── ① "ROLE_GUEST" (문자열)
│       └── ② 문자열을 권한 객체로 감쌈
└── ③ 권한 객체를 리스트에 담음
```

#### ① `"ROLE_" + role.name()` — 권한 이름 만들기

```java
"ROLE_" + Role.GUEST.name()   // "ROLE_GUEST"
"ROLE_" + Role.ADMIN.name()   // "ROLE_ADMIN"
```

Spring Security는 **역할(Role) 권한 이름 앞에 `ROLE_`을 붙이는 규칙**이 있다. `role.name()`은 → "JwtTokenProvider에서 쓴 문법" 섹션 참고.

| SecurityConfig에 쓰는 것 | Spring이 찾는 권한 이름 |
|---|---|
| `hasRole("ADMIN")` | `"ROLE_ADMIN"` (`ROLE_` 자동으로 붙임) |
| `hasAuthority("ROLE_ADMIN")` | `"ROLE_ADMIN"` (그대로) |

`ROLE_`을 빼고 `"ADMIN"`으로 등록하면 `hasRole("ADMIN")`이 `"ROLE_ADMIN"`을 찾다가 **권한 없음(403)**이 된다.

#### ② `new SimpleGrantedAuthority(...)` — 문자열 → 권한 객체

Spring Security는 권한을 문자열이 아니라 **`GrantedAuthority` 타입**으로 다룬다. `SimpleGrantedAuthority`는 그 가장 기본 구현체로, 권한 이름 문자열 하나를 담는다.

#### ③ `List.of(...)` — 리스트에 담기

- 한 사용자가 **권한을 여러 개** 가질 수 있어서, 생성자의 세 번째 파라미터가 컬렉션 타입(`Collection<? extends GrantedAuthority>`)이다. 지금은 사용자당 role이 하나라 원소 1개짜리 리스트
- `List.of` — Java 9부터 있는 **불변 리스트** 생성 메서드. 만든 뒤 추가/삭제 불가 (`UnsupportedOperationException`)

### SecurityContext에 저장

```java
SecurityContextHolder.getContext().setAuthentication(authentication);
```

- `SecurityContextHolder` — 현재 요청(스레드)의 인증 정보를 보관하는 곳
- 여기에 넣어두면 이후 SecurityConfig의 권한 검사, Controller의 사용자 정보 조회에서 이 값을 사용한다
- **변수 범위 주의**: `authentication`은 `if (role != null) { }` 안에서 선언했으므로 저장도 **같은 블록 안**에서 해야 한다. 블록 밖에서 쓰면 컴파일 에러

---

## SecurityConfig

Spring Security 설정 클래스다. JWT 방식에 맞게 기본 기능(세션, 폼 로그인 등)을 끄고, **URL별 접근 권한**을 정하고, **JwtAuthenticationFilter를 등록**한다. 이게 있어야 지금까지 만든 JWT 인증이 실제로 동작한다.

### 코드

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/menus/**").permitAll()
                    .requestMatchers("/", "/login", "/register", "/order", "/order/**",
                            "/css/**", "/js/**", "/images/**").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 클래스 레벨

- `@Configuration` — 설정 클래스. 안의 `@Bean` 메서드들이 반환하는 객체를 빈으로 등록
- `@EnableWebSecurity` — Spring Security 웹 보안 설정 활성화
- `@RequiredArgsConstructor` — `JwtTokenProvider`를 생성자 주입받음 (빈이므로 주입 성공 → "JwtProperties" 섹션의 생성자 주입 참고)

### `@Bean` vs `@Component`

| | `@Component` | `@Bean` |
|---|---|---|
| 붙이는 곳 | **클래스** | `@Configuration` 클래스 안의 **메서드** |
| 등록되는 것 | 그 클래스의 객체 | 메서드가 **반환하는 객체** |
| 쓰는 경우 | 내가 만든 클래스 (`JwtTokenProvider`) | 라이브러리 클래스처럼 클래스에 어노테이션을 못 붙일 때, 생성 방식을 직접 정하고 싶을 때 |

`BCryptPasswordEncoder`, `SecurityFilterChain`은 Spring 라이브러리 클래스라 `@Component`를 붙일 수 없다. 그래서 `@Bean` 메서드로 **직접 만들어서** 등록한다.

### 설정 항목별 설명

| 설정 | 의미 | 왜 |
|---|---|---|
| `csrf.disable()` | CSRF 보호 끄기 | CSRF는 **브라우저가 쿠키를 자동으로 보내는 것**을 악용하는 공격. JWT는 헤더에 직접 넣어 보내므로 해당 없음 |
| `SessionCreationPolicy.STATELESS` | 세션을 만들지도 쓰지도 않음 | 로그인 상태를 서버에 저장하지 않고 **JWT로만** 판단 (수평 확장 가능) |
| `formLogin.disable()` | Spring 기본 로그인 페이지 끄기 | 로그인은 `POST /api/auth/login` API로 직접 구현 |
| `httpBasic.disable()` | HTTP Basic 인증(브라우저 팝업) 끄기 | 사용하지 않음 |
| `authorizeHttpRequests` | URL별 접근 권한 | 아래 참고 |
| `addFilterBefore` | JWT 필터 등록 | 아래 참고 |

### URL별 권한 — `authorizeHttpRequests`

```
POST /api/auth/**   → permitAll      (회원가입, 로그인, 게스트, 재발급, 로그아웃)
GET  /api/menus/**  → permitAll      (메뉴는 로그인 전에도 조회)
페이지·정적 리소스    → permitAll      (/, /login, /register, /order/**, /css/** ...)
그 외 전부           → authenticated  (인증된 사용자만)
```

- `requestMatchers(HttpMethod.POST, "/api/auth/**")` — HTTP 메서드 + URL 패턴으로 매칭. 메서드를 생략하면 모든 메서드
- `**` — 하위 경로 전부 (`/api/auth/login`, `/api/auth/guest` ...)
- `permitAll()` — 인증 없이 누구나
- `authenticated()` — SecurityContext에 인증 정보가 있어야 함. 없으면 거부된다
  - 단, `formLogin`/`httpBasic`을 끄고 별도 설정이 없으면 Spring Security 기본값은 **403**으로 응답한다 (기본 진입점 `Http403ForbiddenEntryPoint`)
  - "인증 안 됨 = 401", "권한 없음 = 403"으로 구분하려면 `AuthenticationEntryPoint`(401)와 `AccessDeniedHandler`(403)를 직접 등록해야 한다 → 응답도 `ApiResponse` 형식으로 통일 가능

#### 순서가 중요하다

규칙은 **위에서부터** 검사해서 **처음 매칭되는 것**이 적용된다.

```java
.anyRequest().authenticated()                         // ❌ 맨 위에 두면
.requestMatchers("/api/menus/**").permitAll()         //    여기까지 오지 않음
```

그래서 `anyRequest()`는 항상 **맨 마지막**에 둔다 (Spring Security 6은 `anyRequest()` 뒤에 규칙을 추가하면 실행 시 에러를 낸다).

#### Filter와의 연결

> **`.anyRequest().authenticated()`가 JwtAuthenticationFilter와 맞물리는 지점이다.** Filter가 SecurityContext에 인증을 넣었으면 통과, 비어 있으면 여기서 막힌다. "401은 Filter가 아니라 SecurityConfig가 판단한다"는 게 이 부분이다.

### 필터 등록 — `addFilterBefore`

```java
.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
        UsernamePasswordAuthenticationFilter.class);
```

- "`UsernamePasswordAuthenticationFilter` **앞에** JWT 필터를 끼워 넣어라"
- 인가(권한) 검사보다 먼저 실행되어야 검사 시점에 SecurityContext가 채워져 있다 (→ "JwtAuthenticationFilter > 필터 체인에서의 위치")
- `new`로 직접 생성 — Filter에 `@Component`를 안 붙였으므로. 생성자에는 필드로 주입받은 `jwtTokenProvider`를 넘긴다

### PasswordEncoder — BCrypt

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

- 회원가입 시 `passwordEncoder.encode(rawPassword)` → 암호화해서 DB 저장
- 로그인 시 `passwordEncoder.matches(rawPassword, encodedPassword)` → 비교
- 반환 타입이 인터페이스(`PasswordEncoder`)라, 나중에 다른 알고리즘으로 바꿔도 사용하는 쪽 코드는 그대로

| | BCrypt | 레거시 (SHA-256 + Salt 직접 구현) |
|---|---|---|
| Salt | 자동 생성, 결과 문자열에 포함 | 직접 생성·저장 |
| 속도 | **일부러 느림** (cost factor) → 무차별 대입 공격에 강함 | 빠름 → 대입 공격에 약함 |
| 같은 비밀번호 | 매번 **다른** 결과 | Salt가 같으면 같은 결과 |

같은 비밀번호도 매번 결과가 달라서 `encode` 결과끼리 `equals`로 비교하면 안 되고, 반드시 `matches()`로 비교해야 한다.

### 람다식 설정 (Lambda DSL)

```java
http.csrf(csrf -> csrf.disable())
```

- `csrf -> csrf.disable()` — **람다식**. "csrf 설정 객체를 받아서 `disable()`을 호출한다"는 함수를 넘기는 것
- Spring Security 6부터 이 방식만 지원한다. 예전 블로그의 `.csrf().disable().and()...` 체이닝 방식은 제거되어 쓸 수 없다

```java
// 예전 방식 (Spring Security 5) — 지금은 컴파일 에러
http.csrf().disable()
    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    .and()
    .authorizeRequests()...

// 현재 방식 (Spring Security 6)
http.csrf(csrf -> csrf.disable())
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth...)
```

> 검색해서 나온 예제가 `.and()`나 `authorizeRequests()`를 쓰면 **옛날 버전 코드**다.

### SecurityConfig에서 쓴 Java 문법

#### ① 람다식 기본 — `매개변수 -> 본문`

```java
csrf -> csrf.disable()
│       └── 본문: 받은 객체로 할 일
└── 매개변수: 메서드가 넘겨주는 설정 객체
```

람다식은 **이름 없는 메서드를 값처럼 넘기는 문법**이다. `http.csrf(...)`는 "CSRF 설정 객체를 줄 테니, 그걸로 뭘 할지 함수로 알려줘"라는 메서드이고, 우리는 그 함수를 람다로 넘긴다.

```java
// 람다 없이 쓰면 (익명 클래스) — 같은 의미
http.csrf(new Customizer<CsrfConfigurer<HttpSecurity>>() {
    @Override
    public void customize(CsrfConfigurer<HttpSecurity> csrf) {
        csrf.disable();
    }
});

// 람다로 쓰면
http.csrf(csrf -> csrf.disable());
```

- **매개변수 이름은 자유**다. `csrf -> csrf.disable()`를 `c -> c.disable()`로 써도 같다. 읽기 쉽게 설정 이름과 맞추는 관례
- **타입을 안 써도 된다**. 컴파일러가 `csrf(...)`의 파라미터 타입을 보고 추론한다
- 본문이 **한 줄**이면 `{}`와 `return`을 생략할 수 있다. 여러 줄이면 `{ ...; }`로 감싼다

#### ② 중첩된 람다와 괄호 범위 — `authorizeHttpRequests`

```java
.authorizeHttpRequests(auth -> auth                          // ← ( 열림, 람다 시작
        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/menus/**").permitAll()
        .requestMatchers("/", "/login", ...).permitAll()
        .anyRequest().authenticated())                       // ← ) 닫힘, 람다 끝
.addFilterBefore(...)                                        // ← 다시 http 체이닝
```

- `auth -> auth.requestMatchers(...)...anyRequest().authenticated()` 전체가 **하나의 람다 본문**이다
- `requestMatchers`, `anyRequest`는 `http`가 아니라 **`auth` 객체의 메서드**다
- `.authenticated())`의 마지막 `)`가 `authorizeHttpRequests(`를 닫는다. 그 뒤의 `.addFilterBefore`는 다시 `http`에 이어지는 체이닝
- 들여쓰기는 컴파일러가 보지 않는다. **괄호 짝**이 범위를 결정하므로, 람다 안쪽 줄은 한 단계 더 들여써서 구분하는 게 좋다

#### ③ 메서드 참조 — `AbstractHttpConfigurer::disable`

`csrf -> csrf.disable()`처럼 **받은 값으로 메서드 하나만 호출**하는 람다는 메서드 참조로 줄일 수 있다.

```java
http.csrf(csrf -> csrf.disable())                 // 람다
http.csrf(AbstractHttpConfigurer::disable)        // 메서드 참조 (같은 의미)
```

`클래스::메서드` 형태이며, 실무 코드에서 자주 보인다. 둘 다 동작은 같다.

#### ④ 메서드 체이닝과 `http.build()`

```java
http.csrf(...)                    // HttpSecurity 반환
    .sessionManagement(...)       // HttpSecurity 반환
    .formLogin(...)               // HttpSecurity 반환
    ...
    .addFilterBefore(...);        // HttpSecurity 반환

return http.build();              // 설정을 바탕으로 SecurityFilterChain 생성
```

- 각 설정 메서드가 **`http` 자신을 반환**해서 `.`으로 이어 쓸 수 있다 (→ "JwtTokenProvider에서 쓴 문법 > 빌더 패턴 + 메서드 체이닝"과 같은 구조)
- `HttpSecurity`는 설정을 모으는 **빌더**이고, 마지막 `build()`가 실제 `SecurityFilterChain` 객체를 만든다 (`Jwts.builder()...compact()`와 같은 흐름)

#### ⑤ `throws Exception`

```java
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
```

`http.build()`와 일부 설정 메서드가 `throws Exception`으로 선언돼 있다. 체크 예외라서 **직접 처리(try-catch)하거나 호출한 쪽으로 넘겨야** 하고, 여기서는 넘긴다. 설정 중 예외가 나면 Spring이 받아서 애플리케이션 시작을 실패시킨다.

#### ⑥ 가변 인자 — `requestMatchers(String... patterns)`

```java
.requestMatchers("/", "/login", "/register", "/order", "/order/**",
        "/css/**", "/js/**", "/images/**").permitAll()
```

- `String...`은 **가변 인자**(varargs). 같은 타입의 값을 개수 제한 없이 쉼표로 나열할 수 있다
- 메서드 안에서는 배열(`String[]`)로 받는다
- 첫 인자로 `HttpMethod`를 넘기는 버전(`requestMatchers(HttpMethod method, String... patterns)`)도 있다 — 메서드 오버로딩

#### ⑦ 상수와 enum — `HttpMethod.POST`, `SessionCreationPolicy.STATELESS`

```java
HttpMethod.POST                   // HTTP 메서드 상수
SessionCreationPolicy.STATELESS   // 세션 정책 enum 상수
```

문자열 `"POST"`, `"STATELESS"` 대신 **미리 정의된 상수**를 쓴다. 오타가 나면 컴파일 에러로 바로 잡힌다 (`Role.GUEST`를 쓰는 것과 같은 이유).

| 상수 | 의미 |
|---|---|
| `SessionCreationPolicy.ALWAYS` | 항상 세션 생성 |
| `SessionCreationPolicy.IF_REQUIRED` | 필요할 때만 생성 (기본값) |
| `SessionCreationPolicy.NEVER` | 만들진 않지만 이미 있으면 사용 |
| `SessionCreationPolicy.STATELESS` | **만들지도 쓰지도 않음** ← JWT |

#### ⑧ 클래스 리터럴 — `UsernamePasswordAuthenticationFilter.class`

```java
.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
        UsernamePasswordAuthenticationFilter.class);
```

- 첫 번째 인자는 **필터 객체** (`new`로 만든 실제 인스턴스)
- 두 번째 인자는 **필터의 타입 정보** (`.class`) — "이 종류의 필터 앞에"라는 **위치 기준**만 알려주는 것이라 객체가 필요 없다
- `클래스명.class`는 → "JwtTokenProvider에서 쓴 문법 > `String.class`란?"과 같은 문법

#### ⑨ 반환 타입을 인터페이스로 — `PasswordEncoder`

```java
public PasswordEncoder passwordEncoder() {    // 반환 타입: 인터페이스
    return new BCryptPasswordEncoder();       // 실제 객체: 구현체
}
```

- 반환 타입은 **인터페이스**(`PasswordEncoder`), 실제로 만드는 건 **구현체**(`BCryptPasswordEncoder`) — 다형성
- 이 빈을 주입받는 쪽(`AuthService`)은 `PasswordEncoder` 타입만 알면 된다. 나중에 다른 알고리즘으로 바꿔도 이 메서드 한 줄만 고치면 되고, 사용하는 쪽 코드는 그대로다
