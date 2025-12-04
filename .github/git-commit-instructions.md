# Git 커밋 메시지 가이드 - Loopers Spring Java Template

이 문서는 Loopers Spring Java 템플릿 프로젝트의 Git 커밋 메시지 작성 및 Git-flow 브랜치 전략에 대한 가이드입니다.

## 커밋 메시지 템플릿

### 기본 구조
```
<타입>(<범위>): <제목>

<본문 (선택사항)>

<푸터 (선택사항)>
```

### 타입 (Type)
- **feat**: 새로운 기능 추가
- **fix**: 버그 수정
- **docs**: 문서 수정
- **style**: 코드 포맷팅, 세미콜론 누락 등 (기능 변경 없음)
- **refactor**: 코드 리팩토링 (기능 변경 없음)
- **test**: 테스트 코드 추가/수정
- **chore**: 빌드 설정, 패키지 매니저 설정 등
- **perf**: 성능 개선
- **ci**: CI/CD 설정 변경
- **revert**: 이전 커밋 되돌리기

### 범위 (Scope) - 도메인별
Loopers 프로젝트 도메인에 따른 범위 구분:
- **user**: 사용자 관리
- **point**: 포인트 관리
- **order**: 주문 관리
- **payment**: 결제 관리
- **product**: 상품 관리
- **auth**: 인증/인가
- **notification**: 알림 관리
- **common**: 공통 기능
- **config**: 설정
- **infra**: 인프라스트럭처
- **test**: 테스트
- **docs**: 문서화
- **monitoring**: 모니터링
- **security**: 보안

### 제목 작성 규칙
- **한국어로 작성**
- **50자 이내**
- **명령문 형태** (예: "사용자 등록 기능 추가")
- **마침표 사용하지 않음**
- **과거형 사용하지 않음** (예: "추가했음" ❌, "추가" ✅)

### 본문 작성 규칙 (선택사항)
- **한국어로 작성**
- **72자마다 줄바꿈**
- **무엇을, 왜 변경했는지 설명**
- **어떻게 변경했는지보다는 왜 변경했는지에 집중**

### 푸터 작성 규칙 (선택사항)
- **이슈 번호 참조**: `Fixes #123`, `Closes #456`
- **Breaking Change**: `BREAKING CHANGE: 사용자 인증 방식 변경`
- **Co-authored-by**: 공동 작성자 표시

## 커밋 메시지 예시

### 기능 추가
```
feat(user): 사용자 등록 기능 추가

- 사용자 정보 입력 폼 구현
- 이메일 중복 검증 로직 추가
- 사용자 등록 API 엔드포인트 구현

Closes #123
```

### 버그 수정
```
fix(point): 포인트 조회 시 null 예외 처리

포인트 이력이 없는 경우 NullPointerException이 발생하는 문제를 
빈 리스트를 반환하도록 수정

Fixes #456
```

### 리팩토링
```
refactor(common): ApiResponse 구조 개선

- 공통 응답 형식 일관성 향상
- Metadata 구조 개선으로 에러 처리 강화
- 제네릭 타입 안정성 강화
```

### 문서 수정
```
docs(readme): API 문서 링크 업데이트

Swagger UI 접속 경로 변경에 따른 README 수정
```

### 테스트 추가
```
test(user): 사용자 도메인 서비스 단위 테스트 추가

- 사용자 생성 로직 테스트
- 사용자 상태 변경 테스트
- 예외 상황 처리 테스트
```

## Git-flow 브랜치 전략

### 브랜치 구조
```
main (운영)
├── develop (개발)
├── feature/* (기능 개발)
├── release/* (릴리스 준비)
├── hotfix/* (긴급 수정)
└── bugfix/* (버그 수정)
```

### 브랜치 네이밍 규칙

#### Feature 브랜치
```
feature/<타입>-<도메인>-<간단한설명>
```
**예시:**
- `feature/add-user-registration`
- `feature/update-point-charge-api`
- `feature/enhance-user-authentication`

#### Release 브랜치
```
release/v<버전번호>
```
**예시:**
- `release/v1.2.0`
- `release/v2.0.0-beta`

#### Hotfix 브랜치
```
hotfix/v<버전번호>-<간단한설명>
```
**예시:**
- `hotfix/v1.1.1-point-calculation-bug`
- `hotfix/v1.1.2-security-patch`

#### Bugfix 브랜치
```
bugfix/<이슈번호>-<간단한설명>
```
**예시:**
- `bugfix/123-user-null-exception`
- `bugfix/456-point-charge-error`

### 브랜치별 커밋 메시지 패턴

#### Feature 브랜치
```
feat(user): 사용자 등록 폼 UI 구현
feat(user): 사용자 등록 API 엔드포인트 추가
feat(user): 사용자명 중복 검증 로직 구현
```

#### Release 브랜치
```
release: v1.2.0 배포 준비
docs: v1.2.0 릴리스 노트 작성
chore: 버전 정보 업데이트
```

#### Hotfix 브랜치
```
hotfix: 포인트 조회 시 성능 이슈 수정
fix(point): 대용량 포인트 이력 조회 최적화
```

### 머지 커밋 메시지

#### Feature → Develop
```
Merge branch 'feature/add-user-registration' into develop

사용자 등록 기능 개발 완료
- 사용자 정보 입력 폼 구현
- 사용자 등록 API 및 유효성 검증 추가
- 이메일 중복 검증 로직 구현
```

#### Release → Main
```
Merge branch 'release/v1.2.0' into main

v1.2.0 릴리스 배포
- 사용자 관리 기능 추가
- 포인트 조회 성능 개선
- 사용자 권한 관리 강화
```

#### Hotfix → Main/Develop
```
Merge branch 'hotfix/v1.1.1-point-calculation-bug' into main

긴급 수정: 포인트 계산 오류 해결
```

## 커밋 작성 시 주의사항

### Do ✅
- **한국어로 명확하게 작성**
- **도메인 범위 명시로 변경 영역 표시**
- **커밋 단위를 작게 유지** (하나의 기능/수정사항)
- **테스트와 함께 커밋**
- **이슈 번호 참조**

### Don't ❌
- **"수정", "변경", "업데이트" 같은 모호한 표현**
- **여러 기능을 한 번에 커밋**
- **WIP(Work In Progress) 커밋을 메인 브랜치에 머지**
- **의미 없는 커밋 메시지** (예: "aaa", "test", "임시")

## 자동화 및 도구

### 커밋 메시지 템플릿 설정
```bash
# Git 커밋 템플릿 설정
git config --global commit.template .gitmessage

# .gitmessage 파일 내용
# <타입>(<범위>): <제목>
# 
# <본문>
# 
# <푸터>
```

### 커밋 메시지 검증 (Husky + Commitlint)
```javascript
// commitlint.config.js
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', [
      'feat', 'fix', 'docs', 'style', 'refactor', 
      'test', 'chore', 'perf', 'ci', 'revert'
    ]],
    'scope-enum': [2, 'always', [
      'user', 'point', 'order', 'payment', 'product',
      'auth', 'notification', 'common', 'config', 'infra',
      'test', 'docs', 'monitoring', 'security'
    ]],
    'subject-max-length': [2, 'always', 50],
    'body-max-line-length': [2, 'always', 72]
  }
};
```

이 가이드를 통해 팀 전체가 일관성 있는 커밋 메시지를 작성하고, 효율적인 Git-flow를 운영할 수 있습니다.
