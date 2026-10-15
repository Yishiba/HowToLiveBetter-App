# 发版

推一个 `v主.次.修订` 的 tag，`.github/workflows/release.yml` 就会依次：

1. 跑全部测试，按测试报告判断成败；
2. 出签名的 APK；
3. 验签，签名证书必须是登记过的那一个；
4. 把 APK、校验和、混淆映射表挂到 GitHub Release。

`versionCode` 由版本号推出：主 × 10000 + 次 × 100 + 修订，所以 `0.1.0` 是 100。只要版本号往上走，它就单调递增，不用手工维护。

## 第一次发版前（只做一次，要你本人来做）

### 推荐：一条命令做完

在你自己的终端里运行（不要交给别人或 AI 代跑）：

```bash
tools/release/setup-signing.sh
```

它会依次：

1. 随机生成一个 32 位密码，全程不显示，也不出现在命令行参数里；
2. 在 `~/LifeGuideRelease/` 生成密钥；
3. 把密码存进 macOS 钥匙串，名字是「LifeGuide 发版密钥」；
4. 把密钥和密码上传成 GitHub Secrets；
5. 把证书指纹登记成仓库变量。

已经生成过密钥的，它会直接停下，不会覆盖。想先看看会发生什么，可以加 `--dry-run`：只在临时目录里生成，不碰钥匙串和 GitHub。

跑完只剩一件事：**备份**（见下面第 2 步）。下面第 1、3、4 步是脚本自动做的事，想手动做时照着来。


### 1. 生成发版密钥

```bash
keytool -genkeypair -keystore lifeguide-release.jks -storetype PKCS12 -alias lifeguide -keyalg RSA -keysize 4096 -validity 10000
```

按提示设密码，填名字。密钥文件**不要**放进仓库目录。

### 2. 备份密钥（最重要的一步）

把 `lifeguide-release.jks` 和密码分开存在两个地方，比如密码管理器加一个离线 U 盘。

**密钥丢了，已经装上的用户就再也收不到更新**：Android 只接受同一把密钥签名的更新，换了密钥就只能让用户卸载重装，清单的打勾记录也会一起丢掉。

### 3. 把密钥交给 GitHub

```bash
base64 -i lifeguide-release.jks | gh secret set LIFEGUIDE_KEYSTORE_BASE64
```

```bash
gh secret set LIFEGUIDE_KEYSTORE_PASSWORD
```

```bash
gh secret set LIFEGUIDE_KEY_ALIAS --body lifeguide
```

```bash
gh secret set LIFEGUIDE_KEY_PASSWORD
```

不带 `--body` 的两条会提示你输入密码。PKCS12 格式的密钥库，两个密码相同。

### 4. 登记签名证书指纹

先查指纹：

```bash
keytool -list -v -keystore lifeguide-release.jks -alias lifeguide | grep SHA256
```

再把查到的指纹登记成仓库变量。指纹是公开信息，所以用变量而不是 Secret，写进 Release 说明里也让用户能核对：

```bash
gh variable set RELEASE_CERT_SHA256 --body "<上一条命令输出的 SHA256 指纹>"
```

验签那一步拿它比对。哪天误用了别的密钥，发版会在这里停下，而不是发出一个用户装不上的包。

## 每次发版

```bash
git tag v0.1.0
```

```bash
git push origin v0.1.0
```

到 Actions 页看「发版」那次运行，成功后 Releases 页会多出：

- `LifeGuide-0.1.0.apk`
- `LifeGuide-0.1.0.apk.sha256`
- `mapping-0.1.0.txt`：混淆映射表，用户报崩溃时把堆栈还原成源码位置要用。

## 本机出签名包

和 CI 读的是同一组环境变量：

```bash
LIFEGUIDE_KEYSTORE=/path/to/lifeguide-release.jks LIFEGUIDE_KEYSTORE_PASSWORD=… LIFEGUIDE_KEY_ALIAS=lifeguide LIFEGUIDE_KEY_PASSWORD=… ./gradlew :androidApp:assembleRelease -PversionName=0.1.0
```

不设这几个变量时，`assembleRelease` 出的是未签名的包，可以用来看混淆后的效果，但装不上手机。

## 第一次公开发版前还要定的

一旦有用户装了，下面这些就改不了了，或者改起来代价很大：

- **包名**：现在是 `app.lifeguide`。包名加签名一起决定「是不是同一个 App」，改包名等于发一个新 App。
- **应用名和图标**：现在叫「人生指南」，用的是系统默认图标。
- **署名与非官方声明**：商店介绍里按 CC BY 4.0 写明原书和作者，并注明是非官方客户端。
