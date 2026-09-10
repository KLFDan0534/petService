import React, { useEffect, useRef, useState } from 'react';

const EASE = 'cubic-bezier(0.23, 1, 0.32, 1)';

export interface RevealProps {
  /** Rendered tag (div / span / li ...). */
  tag?: keyof React.JSX.IntrinsicElements;
  /** Seconds. Combine with `index * 0.05` for staggered groups. */
  delay?: number;
  /** Vertical offset before entering (px). */
  y?: number;
  /** Whether a 6px micro-blur is applied before entering. */
  blur?: boolean;
  /** Whether the reveal plays only once. */
  once?: boolean;
  className?: string;
  style?: React.CSSProperties;
  children?: React.ReactNode;
}

function reduceMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches);

}

export function Reveal({
  tag = 'div',
  delay = 0,
  y = 18,
  blur = true,
  once = true,
  className,
  style,
  children
}: RevealProps) {
  const el = useRef<HTMLElement | null>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (reduceMotion()) {
      setVisible(true);
      return;
    }
    if (!('IntersectionObserver' in window)) {
      setVisible(true);
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setVisible(true);
            if (once) {
              observer.unobserve(entry.target);
              observer.disconnect();
            }
          } else if (!once) {
            setVisible(false);
          }
        });
      },
      { rootMargin: '-10% 0px -12% 0px', threshold: 0 }
    );

    if (el.current) observer.observe(el.current);

    return () => observer.disconnect();
  }, [once]);

  const Tag = tag as unknown as React.ElementType;

  return (
    <Tag
      ref={el}
      className={className}
      style={{
        display: 'block',
        opacity: visible ? 1 : 0,
        transform: visible ? 'translateY(0)' : `translateY(${y}px)`,
        filter: visible ? 'blur(0)' : `blur(${blur ? 6 : 0}px)`,
        transition: `opacity 0.3s ${EASE}, transform 0.3s ${EASE}, filter 0.3s ${EASE}`,
        transitionDelay: delay ? `${delay}s` : undefined,
        willChange: 'opacity, transform, filter',
        ...style
      }}>
      
      {children}
    </Tag>);

}