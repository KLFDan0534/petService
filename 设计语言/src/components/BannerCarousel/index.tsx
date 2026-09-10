import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ArrowLeftIcon, ArrowRightIcon, ImageIcon } from 'lucide-react';

export type Banner = {
  /** Stable id from the platform banner record. */
  id_wsh: string | number;
  title_wsh?: string;
  image_url_wsh?: string;
  link_url_wsh?: string;
  /** `0` hides the banner from the rotation. */
  status_wsh?: number;
};

export type BannerCarouselProps = {
  banners?: Banner[];
  /** Auto-rotate interval in ms. Defaults to the product's 6000ms. */
  interval?: number;
  className?: string;
};

const SCRIM_DESKTOP =
'linear-gradient(90deg, rgba(20, 25, 28, 0.78) 0%, rgba(20, 25, 28, 0.36) 48%, rgba(20, 25, 28, 0.08) 75%)';
const SCRIM_MOBILE =
'linear-gradient(0deg, rgba(20, 25, 28, 0.82) 0%, rgba(20, 25, 28, 0.18) 75%)';

export function toSafeUrl(value?: string): string {
  const rawUrl = String(value || '').trim();
  if (!rawUrl || typeof window === 'undefined') return '';

  try {
    const parsedUrl = new URL(rawUrl, window.location.origin);
    return ['http:', 'https:'].includes(parsedUrl.protocol) ? parsedUrl.href : '';
  } catch {
    return '';
  }
}

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

export function BannerCarousel({
  banners = [],
  interval = 6000,
  className = ''
}: BannerCarouselProps) {
  const slides = useMemo(
    () => banners.filter((banner) => banner?.id_wsh && banner.status_wsh !== 0),
    [banners]
  );

  const [activeIndex, setActiveIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    setActiveIndex(0);
  }, [slides]);

  const currentBanner = slides[activeIndex] || {} as Banner;
  const safeCurrentLink = toSafeUrl(currentBanner.link_url_wsh);

  const showSlide = useCallback(
    (index: number) => {
      if (!slides.length) return;
      setActiveIndex((index % slides.length + slides.length) % slides.length);
    },
    [slides.length]
  );

  const showPrevious = () => showSlide(activeIndex - 1);
  const showNext = () => showSlide(activeIndex + 1);

  useEffect(() => {
    const prefersReducedMotion =
    typeof window !== 'undefined' &&
    window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

    if (isPaused || slides.length <= 1 || prefersReducedMotion) return;

    timerRef.current = window.setInterval(() => {
      setActiveIndex((previous) => (previous + 1) % slides.length);
    }, interval);

    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current);
      timerRef.current = null;
    };
  }, [isPaused, slides.length, interval, activeIndex]);

  if (!slides.length) return null;

  return (
    <section
      aria-label="平台广告"
      className={`relative w-full min-h-[260px] aspect-[16/11] md:min-h-[220px] md:aspect-[4/1] overflow-hidden rounded-frame border border-line bg-sand shadow-lift ${className}`}
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      onFocus={() => setIsPaused(true)}
      onBlur={() => setIsPaused(false)}>
      
      <AnimatePresence mode="wait" initial={false}>
        <motion.a
          key={currentBanner.id_wsh}
          href={safeCurrentLink || undefined}
          target={safeCurrentLink ? '_blank' : undefined}
          rel={safeCurrentLink ? 'noopener noreferrer' : undefined}
          aria-label={
          safeCurrentLink ? `查看广告：${currentBanner.title_wsh}` : undefined
          }
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.22, ease: [0.23, 1, 0.32, 1] }}
          className={`absolute inset-0 block h-full w-full text-white ${
          safeCurrentLink ? 'cursor-pointer' : 'cursor-default'}`
          }>
          
          <BannerMedia
            src={currentBanner.image_url_wsh}
            alt={currentBanner.title_wsh || '平台广告图片'}
            placeholder={currentBanner.title_wsh || '平台广告'} />
          

          {/* Legibility scrim over the photography: bottom-up on mobile, left-to-right above md. */}
          <span
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 md:hidden"
            style={{ background: SCRIM_MOBILE }} />
          
          <span
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 hidden md:block"
            style={{ background: SCRIM_DESKTOP }} />
          

          <span className="absolute z-[1] bottom-8 left-5 right-5 md:bottom-7 md:right-auto md:left-[clamp(24px,5vw,64px)] md:max-w-[min(560px,calc(100%-120px))] flex flex-col items-start gap-[9px]">
            <span className="inline-flex rounded-tile bg-white px-[9px] py-[5px] text-[12px] font-semibold tracking-label text-[#17202a]">
              广告 · 平台推荐
            </span>
            <strong
              className="font-display font-medium leading-[1.15] tracking-editorial ref-clamp-2"
              style={{
                fontSize: 'clamp(24px, 3vw, 42px)',
                textWrap: 'balance' as React.CSSProperties['textWrap']
              }}>
              
              {currentBanner.title_wsh || '平台精选活动'}
            </strong>
            {safeCurrentLink ?
            <span className="group inline-flex min-h-[44px] items-center gap-[7px] text-[14px] font-semibold text-white">
                查看活动
                <ArrowRightIcon
                aria-hidden="true"
                className="h-4 w-4 transition-transform duration-normal ease-editorial group-hover:translate-x-1" />
              
              </span> :
            null}
          </span>
        </motion.a>
      </AnimatePresence>

      {slides.length > 1 ?
      <>
          <button
          type="button"
          aria-label="上一条广告"
          onClick={showPrevious}
          className="absolute z-[2] top-[18px] right-[62px] h-10 w-10 md:top-1/2 md:right-auto md:left-[14px] md:h-11 md:w-11 md:-translate-y-1/2 grid place-items-center rounded-tile border border-white/80 bg-white/95 text-[#17202a] shadow-[0_5px_16px_rgba(0,0,0,0.18)] transition-all duration-fast ease-editorial hover:bg-white hover:scale-[1.04] md:hover:scale-[1.04] active:translate-y-px">
          
            <ArrowLeftIcon aria-hidden="true" className="h-[18px] w-[18px]" />
          </button>
          <button
          type="button"
          aria-label="下一条广告"
          onClick={showNext}
          className="absolute z-[2] top-[18px] right-[14px] h-10 w-10 md:top-1/2 md:h-11 md:w-11 md:-translate-y-1/2 grid place-items-center rounded-tile border border-white/80 bg-white/95 text-[#17202a] shadow-[0_5px_16px_rgba(0,0,0,0.18)] transition-all duration-fast ease-editorial hover:bg-white hover:scale-[1.04] active:translate-y-px">
          
            <ArrowRightIcon aria-hidden="true" className="h-[18px] w-[18px]" />
          </button>

          <div
          role="tablist"
          aria-label="广告切换"
          className="absolute z-[2] bottom-4 right-[18px] flex gap-2">
          
            {slides.map((banner, index) =>
          <button
            key={banner.id_wsh}
            type="button"
            role="tab"
            aria-label={`查看第 ${index + 1} 条广告`}
            aria-selected={activeIndex === index}
            aria-current={activeIndex === index ? 'true' : undefined}
            onClick={() => showSlide(index)}
            className={`h-2 rounded-[4px] border-0 p-0 transition-all duration-fast ease-editorial ${
            activeIndex === index ? 'w-9 bg-white' : 'w-6 bg-white/60 hover:bg-white/80'}`
            } />

          )}
          </div>
        </> :
      null}
    </section>);

}

type BannerMediaProps = {
  src?: string;
  alt?: string;
  placeholder?: string;
};

function BannerMedia({ src, alt = '', placeholder = '' }: BannerMediaProps) {
  const [failed, setFailed] = useState(false);
  const displaySrc = normalizeMediaUrl(src);

  useEffect(() => {
    setFailed(false);
  }, [src]);

  const showFallback = !displaySrc || failed;

  return (
    <div className="relative h-full w-full min-w-0 overflow-hidden bg-sand">
      {!showFallback ?
      <img
        src={displaySrc}
        alt={alt}
        loading="eager"
        decoding="async"
        onError={() => setFailed(true)}
        className="block h-full w-full object-cover" /> :


      <div
        role={alt ? 'img' : undefined}
        aria-label={alt || undefined}
        className="flex h-full w-full flex-col items-center justify-center gap-2 bg-cream p-gutter text-center text-muted">
        
          <ImageIcon aria-hidden="true" className="h-9 w-9 text-brand" />
          {placeholder ?
        <span className="text-meta font-medium ref-clamp-2">{placeholder}</span> :
        null}
        </div>
      }
    </div>);

}