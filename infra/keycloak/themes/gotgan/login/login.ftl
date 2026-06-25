<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>곳간 로그인</title>
    <link rel="icon" type="image/svg+xml" href="${url.resourcesPath}/img/logo.svg">
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
</head>
<body>
<main class="page">
    <section class="card">
        <div class="brand">
            <svg viewBox="0 0 32 32" width="30" height="30" fill="#b5703c" aria-hidden="true">
                <path d="M16 3 L29 12 L3 12 Z"/>
                <rect x="8" y="18" width="4" height="8" rx="1"/>
                <rect x="14" y="16" width="4" height="10" rx="1"/>
                <rect x="20" y="14" width="4" height="12" rx="1"/>
            </svg>
            <span class="brand-name">곳간</span>
        </div>
        <p class="tagline">자산을 갈무리하는 곳</p>

        <#if message?? && message.summary?has_content>
            <div class="alert alert-${message.type!'info'}">${message.summary}</div>
        </#if>

        <form action="${url.loginAction}" method="post" class="form" novalidate>
            <label for="username">아이디</label>
            <input id="username" name="username" type="text" autocomplete="username"
                   value="${(login.username)!''}" autofocus>

            <label for="password">비밀번호</label>
            <input id="password" name="password" type="password" autocomplete="current-password">

            <button type="submit" class="submit">로그인</button>
        </form>
    </section>
</main>
</body>
</html>
