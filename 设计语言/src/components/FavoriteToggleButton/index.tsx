import { useCallback, useEffect, useRef, useState } from 'react';
import { Loader2Icon, StarIcon } from 'lucide-react';

/* ------------------------------------------------------------------ *
 * Ported from src/constants/favorite.js
 * ------------------------------------------------------------------ */

export const FAVORITE_TARGET_TYPES = {
  MERCHANT: 'merchant',
  KEEPER: 'keeper',
  SERVICE: 'service'
} as const;

export type FavoriteTargetType =
(typeof FAVORITE_TARGET_TYPES)[keyof typeof FAVORITE_TARGET_TYPES];

export const FAVORITE_TARGET_TYPE_LABELS: Record<string, string> = {
  merchant: '商家',
  keeper: '寄养员',
  service: '服务'
};

export function normalizeFavoriteTargetType(value: unknown): string {
  return String(value ?? '').
  trim().
  toLowerCase();
}

export function isFavoriteTargetType(value: unknown): boolean {
  const normalized = normalizeFavoriteTargetType(value);
  return (Object.values(FAVORITE_TARGET_TYPES) as string[]).includes(normalized);
}

export function getFavoriteTargetTypeLabel(value: unknown): string {
  return FAVORITE_TARGET_TYPE_LABELS[normalizeFavoriteTargetType(value)] || '未知';
}

/* ------------------------------------------------------------------ *
 * FavoriteToggleButton
 * ------------------------------------------------------------------ */

export type FavoriteToastTone = 'success' | 'error';

export interface FavoriteToggleButtonProps {
  /** Id of the favourited record. Must resolve to a positive number. */
  targetId: number | string;
  /** One of `merchant` | `keeper` | `service`. */
  targetType: string;
  /** Render the label next to the star. */
  showText?: boolean;
  favoritedLabel?: string;
  unfavoritedLabel?: string;
  /** Initial value used until (or instead of) `onCheckFavorite` resolves. */
  defaultFavorited?: boolean;
  /** Fetches the current favourite state on mount / when the target changes. */
  onCheckFavorite?: (
  targetId: number,
  targetType: string)
  => Promise<boolean> | boolean;
  /** Persists the toggle. Rejecting reverts the optimistic flip. */
  onToggle?: (
  next: boolean,
  targetId: number,
  targetType: string)
  => Promise<unknown> | unknown;
  /** When false, clicking calls `onRequireLogin` instead of toggling. */
  isAuthenticated?: boolean;
  onRequireLogin?: () => void;
  /** Toast hook — mirrors the source's `appStore.addToast`. */
  onNotify?: (message: string, tone: FavoriteToastTone) => void;
  disabled?: boolean;
  className?: string;
}

export function FavoriteToggleButton({
  targetId,
  targetType,
  showText = true,
  favoritedLabel = '已收藏',
  unfavoritedLabel = '收藏',
  defaultFavorited = false,
  onCheckFavorite,
  onToggle,
  isAuthenticated = true,
  onRequireLogin,
  onNotify,
  disabled = false,
  className = ''
}: FavoriteToggleButtonProps) {
  const numericId = Number(targetId);
  const normalizedType = normalizeFavoriteTargetType(targetType);
  const targetReady = numericId > 0 && isFavoriteTargetType(normalizedType);
  const syncEnabled = isAuthenticated && targetReady;

  const [isFavorited, setIsFavorited] = useState(defaultFavorited);
  const [loading, setLoading] = useState(Boolean(onCheckFavorite) && syncEnabled);
  const [toggling, setToggling] = useState(false);
  const requestId = useRef(0);

  useEffect(() => {
    if (!onCheckFavorite || !syncEnabled) {
      setIsFavorited(defaultFavorited);
      setLoading(false);
      return;
    }
    const current = ++requestId.current;
    setLoading(true);
    Promise.resolve(onCheckFavorite(numericId, normalizedType)).
    then((next) => {
      if (current === requestId.current) setIsFavorited(Boolean(next));
    }).
    catch(() => {
      if (current === requestId.current) setIsFavorited(false);
    }).
    finally(() => {
      if (current === requestId.current) setLoading(false);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [numericId, normalizedType, syncEnabled, onCheckFavorite]);

  const handleClick = useCallback(
    async (event: React.MouseEvent<HTMLButtonElement>) => {
      event.stopPropagation();
      if (!isAuthenticated) {
        onRequireLogin?.();
        return;
      }
      if (!targetReady) {
        onNotify?.('不支持的收藏类型', 'error');
        return;
      }
      const next = !isFavorited;
      setIsFavorited(next);
      setToggling(true);
      try {
        await onToggle?.(next, numericId, normalizedType);
        onNotify?.(next ? '已收藏' : '已取消收藏', 'success');
      } catch {
        setIsFavorited(!next);
        onNotify?.('操作失败', 'error');
      } finally {
        setToggling(false);
      }
    },
    [
    isAuthenticated,
    isFavorited,
    normalizedType,
    numericId,
    onNotify,
    onRequireLogin,
    onToggle,
    targetReady]

  );

  const busy = loading || toggling;
  const buttonDisabled = disabled || busy || !targetReady;
  const label = isFavorited ? favoritedLabel : unfavoritedLabel;

  return (
    <button
      type="button"
      aria-label={label}
      aria-pressed={isFavorited}
      aria-busy={busy || undefined}
      disabled={buttonDisabled}
      onClick={handleClick}
      className={[
      'group inline-flex h-10 shrink-0 items-center justify-center gap-1.5 rounded-full border border-transparent bg-transparent',
      showText ? 'px-3' : 'w-10 px-0',
      'text-control transition-colors duration-fast ease-editorial',
      isFavorited ? 'text-caution' : 'text-muted',
      'hover:enabled:bg-sand hover:enabled:text-caution',
      'focus-visible:shadow-focus active:enabled:translate-y-px',
      'disabled:cursor-not-allowed disabled:opacity-50',
      className].

      filter(Boolean).
      join(' ')}>
      
      {busy ?
      <Loader2Icon className="h-5 w-5 animate-spin" aria-hidden="true" /> :

      <StarIcon
        className={[
        'h-5 w-5 transition-transform duration-normal ease-editorial',
        buttonDisabled ? '' : 'group-hover:scale-110'].

        filter(Boolean).
        join(' ')}
        fill={isFavorited ? 'currentColor' : 'none'}
        aria-hidden="true" />

      }
      {showText ? <span className="ref-truncate">{label}</span> : null}
    </button>);

}