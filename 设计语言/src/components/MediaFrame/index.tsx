import React, { useEffect, useState } from 'react';
import { ImageIcon } from 'lucide-react';

export type MediaFrameRatio = '4/3' | '1/1' | '16/9' | '3/2' | '3/4' | 'auto';
export type MediaFrameRadius = 'frame' | 'card' | 'control' | 'tile' | 'none';

export interface MediaFrameProps {
  /** Image source. Empty / failing sources fall back to the placeholder. */
  src?: string;
  /** Alt text. Pass '' for decorative media. */
  alt?: string;
  /** Aspect ratio of the frame. Defaults to the confirmed 4/3 hero ratio. */
  ratio?: MediaFrameRatio;
  /** Corner radius token. Defaults to `frame` (26px), the hero frame radius. */
  radius?: MediaFrameRadius;
  /** Shows the shimmer skeleton instead of the image while data loads. */
  loading?: boolean;
  /** Native image loading strategy. */
  imgLoading?: 'lazy' | 'eager';
  /** Scale the image to 1.06 on hover of the frame or its parent `group`. */
  zoomOnHover?: boolean;
  /** Label rendered under the placeholder icon when there is no image. */
  fallbackLabel?: string;
  /** Overlay content (badges, captions) rendered above the media. */
  children?: React.ReactNode;
  className?: string;
}

const RATIO_CLASS: Record<MediaFrameRatio, string> = {
  '4/3': 'aspect-[4/3]',
  '1/1': 'aspect-square',
  '16/9': 'aspect-[16/9]',
  '3/2': 'aspect-[3/2]',
  '3/4': 'aspect-[3/4]',
  auto: ''
};

const RADIUS_CLASS: Record<MediaFrameRadius, string> = {
  frame: 'rounded-frame',
  card: 'rounded-card',
  control: 'rounded-control',
  tile: 'rounded-tile',
  none: ''
};

export function MediaFrame({
  src = '',
  alt = '',
  ratio = '4/3',
  radius = 'frame',
  loading = false,
  imgLoading = 'lazy',
  zoomOnHover = true,
  fallbackLabel,
  children,
  className = ''
}: MediaFrameProps) {
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    setFailed(false);
  }, [src]);

  const displaySrc = normalizeMediaUrl(src);
  const showFallback = !displaySrc || failed;

  return (
    <div
      className={[
      'group/media relative w-full min-w-0 overflow-hidden border border-line bg-surface',
      RATIO_CLASS[ratio],
      RADIUS_CLASS[radius],
      className].

      filter(Boolean).
      join(' ')}>
      
      {loading ?
      <div className="ref-shimmer h-full w-full" aria-hidden="true" /> :
      showFallback ?
      <div
        className="flex h-full w-full flex-col items-center justify-center gap-2 bg-sand p-gutter text-center text-muted"
        role={alt ? 'img' : undefined}
        aria-label={alt || undefined}>
        
          <ImageIcon className="h-9 w-9 text-brand" aria-hidden="true" strokeWidth={1.5} />
          {fallbackLabel ?
        <span className="text-meta font-medium">{fallbackLabel}</span> :
        null}
        </div> :

      <img
        src={displaySrc}
        alt={alt}
        loading={imgLoading}
        decoding="async"
        onError={() => setFailed(true)}
        className={[
        'block h-full w-full object-cover transition-transform duration-slow ease-editorial',
        zoomOnHover ?
        'group-hover/media:scale-[1.06] group-hover:scale-[1.06]' :
        ''].

        filter(Boolean).
        join(' ')} />

      }

      {children}
    </div>);

}

/** Rewrites local MinIO URLs to same-origin paths, matching the product's media loader. */
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
  } catch {
    return rawUrl;
  }

  return rawUrl;
}