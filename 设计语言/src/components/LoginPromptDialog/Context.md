# LoginPromptDialog

Global auth-gate dialog. Mount it once near the app root and show it whenever a guest triggers a members-only action (favouriting, booking, checkout). Ported from `src/components/common/LoginPromptDialog.vue`.

- Renders through a portal into `document.body` (the Vue `<Teleport to="body">` equivalent).
- Centered, 400px-max card on a blurred scrim; lock glyph, one line of copy, dismiss + login actions.
- Closes on scrim click, `Escape`, or the dismiss button. Confirm closes first, then calls `onLogin` with the resolved login path.
- `safeRedirect` (also exported) rejects non-path and protocol-relative values, falling back to `/dashboard` — keeps the redirect same-origin (F-NAV-006).

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `visible` | `boolean` | — | Controls mounting/visibility. |
| `onClose` | `() => void` | — | Scrim, Escape, and dismiss button. |
| `onLogin` | `(loginPath: string) => void` | — | Receives `/login?redirect=<encoded>`; wire to your router. |
| `loginRedirectPath` | `string` | `/dashboard` | Path to return to after sign-in; sanitised. |
| `message` | `string` | `登录后即可使用该功能` | Body copy. |
| `cancelLabel` | `string` | `暂不登录` | Dismiss label. |
| `confirmLabel` | `string` | `去登录` | Confirm label. |

## Usage

```tsx
import { useNavigate } from 'react-router-dom'
import { LoginPromptDialog } from 'components/LoginPromptDialog'

const navigate = useNavigate()

<LoginPromptDialog
  visible={app.showLoginPrompt}
  onClose={app.closeLoginPrompt}
  loginRedirectPath={app.loginRedirectPath}
  onLogin={path => navigate(path)}
/>
```

## Guidelines

- One instance per app; drive it from app state rather than rendering it per feature.
- Keep the copy to a single sentence — this is an interruption, not a marketing surface.
- Confirm action is the only brand-filled button; dismiss stays outlined.
