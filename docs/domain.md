## 도메인 모델

### 사용자 (User)

#### 속성
- id : Long
- username : 사용자 id (영문 및 숫자 10자 이내)
- email : 이메일 (xx@yy.zz)
- birthDate : 생년월일 (yyyy-mm-dd)

#### 규칙
- username은 고유해야 한다.
- email은 고유해야 한다.
- 속성에 정의된 양식을 준수해서 엔티티를 저장 해야한다.