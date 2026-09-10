import { useEffect, useMemo, useState } from 'react';
import { ImageIcon } from 'lucide-react';

export type MediaWithFallbackProps = {
  /** Image source. Empty / failing sources render the fallback placeholder. */
  src?: string;
  /** Accessible description. Also labels the fallback placeholder when present. */
  alt?: string;
  /** Optional caption shown inside the fallback placeholder (e.g. "暂无图片"). */
  placeholder?: string;
  /** Native image loading strategy. */
  loading?: 'lazy' | 'eager';
  /** Extra classes for the frame element. */
  className?: string;
};

/**
 * Rewrites local MinIO URLs to a relative path so they resolve through the
 * frontend proxy. Ported 1:1 from the product's `normalizeMediaUrl`.
 */
export function normalizeMediaUrl(value?: string): string {
  const rawUrl = String(value || '').trim();
  if (!rawUrl || typeof window === 'undefined') return rawUrl;

  try {
    const parsedUrl = new URL(rawUrl, window.location.origin);
    const isLocalMinioUrl =
    ['localhost', '127.0.0.1'].includes(parsedUrl.hostname) &&
    parsedUrl.pathname.startsWith('/minio/');

    if (isLocalMinioUrl) {
      return `${parsedUrl.pathname}${parsedUrl.search}${parsedUrl.hash}`;
    }
  } catch (_) {
    return rawUrl;
  }

  return rawUrl;
}

export function MediaWithFallback({
  src = '',
  alt = '',
  placeholder = '',
  loading = 'lazy',
  className = ''
}: MediaWithFallbackProps) {
  const [failed, setFailed] = useState(false);

  const displaySrc = useMemo(() => normalizeMediaUrl(src), [src]);
  const showFallback = !displaySrc || failed;

  // Retry whenever the source changes.
  useEffect(() => {
    setFailed(false);
  }, [src]);

  return (
    <div
      className={`relative h-full w-full min-w-0 overflow-hidden bg-sand ${className}`.trim()}>
      
      {!showFallback ?
      <img
        className="block h-full w-full object-cover"
        src={displaySrc}
        alt={alt}
        loading={loading}
        decoding="async"
        onError={() => setFailed(true)} /> :


      <div
        className="flex h-full w-full min-h-[inherit] flex-col items-center justify-center gap-2 p-gutter text-center text-muted"
        style={{
          background:
          'color-mix(in srgb, var(--ref-brand) 10%, var(--ref-surface))'
        }}
        role={alt ? 'img' : undefined}
        aria-label={alt || undefined}>
        
          <ImageIcon size={36} className="text-brand" aria-hidden="true" />
          {placeholder ?
        <span className="text-meta font-bold">{placeholder}</span> :
        null}
        </div>
      }
    </div>);

}