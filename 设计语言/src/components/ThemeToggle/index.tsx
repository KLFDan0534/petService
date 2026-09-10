import { useCallback, useEffect, useState } from 'react';
import { MoonIcon, SunIcon } from 'lucide-react';

export type ThemeMode = 'light' | 'dark';

export type ThemeToggleProps = {
  /** Controlled theme. Omit to let the toggle own its state. */
  theme?: ThemeMode;
  /** Fired with the next theme after every toggle. */
  onThemeChange?: (theme: ThemeMode) => void;
  /**
   * 36px in the merchant / admin chrome, 44px in the user header
   * (`.header-controls :deep(.theme-toggle)` in the source).
   */
  size?: 'sm' | 'md';
  disabled?: boolean;
  className?: string;
};

const THEME_STORAGE_KEY = 'pet-service-theme';
const THEME_MODES: ThemeMode[] = ['light', 'dark'];

export function normalizeTheme(value: unknown): ThemeMode {
  return THEME_MODES.includes(value as ThemeMode) ? value as ThemeMode : 'light';
}

function readStoredTheme(): ThemeMode {
  if (typeof document === 'undefined') return 'light';
  const fromDom = document.documentElement.dataset.theme;
  if (THEME_MODES.includes(fromDom as ThemeMode)) return fromDom as ThemeMode;
  try {
    return normalizeTheme(localStorage.getItem(THEME_STORAGE_KEY));
  } catch {
    return 'light';
  }
}

/** Mirrors `applyTheme` + `setTheme` from the product's app store. */
export function applyTheme(nextTheme: ThemeMode): ThemeMode {
  const theme = normalizeTheme(nextTheme);
  if (typeof document !== 'undefined') {
    const root = document.documentElement;
    root.dataset.theme = theme;
    root.classList.toggle('dark', theme === 'dark');
    root.style.colorScheme = theme;
  }
  try {
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  } catch {

    /* storage unavailable — theme still applies for this session */}
  return theme;
}

const SIZES: Record<'sm' | 'md', string> = {
  sm: 'h-9 w-9 flex-[0_0_36px]',
  md: 'h-11 w-11 flex-[0_0_44px]'
};

export function ThemeToggle({
  theme: themeProp,
  onThemeChange,
  size = 'sm',
  disabled = false,
  className = ''
}: ThemeToggleProps) {
  const [internalTheme, setInternalTheme] = useState<ThemeMode>('light');
  const isControlled = themeProp !== undefined;
  const theme = isControlled ? normalizeTheme(themeProp) : internalTheme;
  const isDarkTheme = theme === 'dark';
  const label = isDarkTheme ? '切换到日间主题' : '切换到夜间主题';

  useEffect(() => {
    if (isControlled) return;
    setInternalTheme(readStoredTheme());
  }, [isControlled]);

  const toggleTheme = useCallback(() => {
    const next: ThemeMode = isDarkTheme ? 'light' : 'dark';
    applyTheme(next);
    if (!isControlled) setInternalTheme(next);
    onThemeChange?.(next);
  }, [isDarkTheme, isControlled, onThemeChange]);

  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      aria-pressed={isDarkTheme}
      disabled={disabled}
      onClick={toggleTheme}
      className={[
      'inline-flex items-center justify-center border-0 bg-transparent text-ink',
      'rounded-control transition-colors duration-fast ease-editorial',
      'hover:bg-transparent hover:text-brand',
      'focus-visible:outline-none',
      'disabled:cursor-not-allowed disabled:text-muted disabled:hover:text-muted',
      SIZES[size],
      className].

      filter(Boolean).
      join(' ')}>
      
      {isDarkTheme ?
      <MoonIcon aria-hidden="true" className="h-[18px] w-[18px]" strokeWidth={1.75} /> :

      <SunIcon aria-hidden="true" className="h-[18px] w-[18px]" strokeWidth={1.75} />
      }
    </button>);

}