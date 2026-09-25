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
