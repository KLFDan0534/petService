import React from 'react';
import {
  AlertTriangleIcon,
  ArrowUpRightIcon,
  ClockIcon,
  PawPrintIcon,
  Trash2Icon } from
'lucide-react';

/* -------------------------------------------------------------------------
 * Types — field names preserved 1:1 from the Vue source (`*_wsh` suffixes).
 * ---------------------------------------------------------------------- */

export type PetType =
'dog' |
'cat' |
'rabbit' |
'bird' |
'fish' |
'hamster' |
'other' |
string;

export interface Pet {
  id_wsh: string | number;
  name_wsh: string;
  type_wsh?: PetType;
  breed_wsh?: string | null;
  avatar_wsh?: string | null;
  age_wsh?: number | string | null;
  weight_wsh?: number | string | null;
  /** 0 未知 · 1 公 · 2 母 */
  gender_wsh?: number | null;
  /** 1 = 已免疫 */
  vaccinated_wsh?: number | null;
  /** 1 = 已绝育 */
  sterilized_wsh?: number | null;
  description_wsh?: string | null;
  habits_wsh?: string | null;
  allergies_wsh?: string | null;
}

export interface PetListProps {
  /** Pet roster to render. */
  pets: Pet[];
  /** Fired when a card, the pet name, or 查看档案 is activated. */
  onView?: (id: Pet['id_wsh']) => void;
  /** Fired with the full pet record when 编辑 is activated. */
  onEdit?: (pet: Pet) => void;
  /** Fired when 删除 is activated. */
  onDelete?: (id: Pet['id_wsh']) => void;
  /** Replaces the grid with a skeleton placeholder set. */
  loading?: boolean;
  /** Rendered when `pets` is empty and not loading. */
  emptyAction?: React.ReactNode;
  className?: string;
}

/* -------------------------------------------------------------------------
 * Component
 * ---------------------------------------------------------------------- */

export function PetList({
  pets,
  onView,
  onEdit,
  onDelete,
  loading = false,
  emptyAction,
  className = ''
}: PetListProps) {
  if (loading) {
    return (
      <div
        className={`grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3 ${className}`}
        aria-busy="true"
        aria-live="polite">
        
        {[0, 1, 2].map((key) =>
        <PetCardSkeleton key={key} />
        )}
      </div>);

  }

  if (!pets.length) {
    return (
      <div
        className={`rounded-card border border-line bg-surface px-6 py-14 text-center ${className}`}>
        
        <span className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-sand text-muted">
          <PawPrintIcon className="h-5 w-5" aria-hidden="true" />
        </span>
        <h3 className="mt-4 font-display text-display-xs text-ink">暂无宠物档案</h3>
        <p className="mx-auto mt-2 max-w-prose text-meta text-ink-soft">
          添加宠物后，服务人员可以提前了解它的品种、健康与忌口情况。
        </p>
        {emptyAction ? <div className="mt-5">{emptyAction}</div> : null}
      </div>);

  }

  return (
    <ul
      className={`grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3 ${className}`}>
      
      {pets.map((pet) =>
      <li key={pet.id_wsh} className="h-full">
          <article
          onClick={() => onView?.(pet.id_wsh)}
          className="group flex h-full cursor-pointer flex-col rounded-card border border-line bg-surface p-5 transition-all duration-normal ease-editorial hover:-translate-y-1 hover:border-brand/40 hover:shadow-lift">
          
            {/* Header: Avatar + Name */}
            <header className="flex items-start gap-3.5">
              <div className="shrink-0">
                {pet.avatar_wsh ?
              <img
                className="h-14 w-14 rounded-full border border-line object-cover"
                src={pet.avatar_wsh}
                alt={pet.name_wsh}
                loading="lazy" /> :


              <span
                className="flex h-14 w-14 items-center justify-center rounded-full border border-line bg-sand text-muted"
                aria-label={`${pet.name_wsh}（${petTypeLabel(
                  pet.type_wsh
                )}）暂无头像`}>
                
                    <PetTypeGlyph type={pet.type_wsh} />
                  </span>
              }
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-1.5">
                  <button
                  type="button"
                  onClick={(event) => {
                    event.stopPropagation();
                    onView?.(pet.id_wsh);
                  }}
                  className="ref-truncate rounded-[4px] text-left text-[16px] font-semibold text-ink hover:underline">
                  
                    {pet.name_wsh}
                  </button>
                  <ArrowUpRightIcon
                  className="h-3.5 w-3.5 shrink-0 text-muted opacity-0 transition-opacity duration-normal ease-editorial group-hover:opacity-100"
                  aria-hidden="true" />
                
                </div>
                <p className="mt-0.5 text-[14px] text-muted">
                  {petTypeLabel(pet.type_wsh)}
                  <span className="mx-1.5 text-line">·</span>
                  {pet.breed_wsh || '未知品种'}
                </p>
              </div>

              {!profileComplete(pet) &&
            <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-brand/10 px-2.5 py-1 text-[12px] font-medium text-brand">
                  <ClockIcon className="h-3 w-3" aria-hidden="true" />
                  档案待完善
                </span>
            }
            </header>

            {/* Specs */}
            <dl className="my-4 grid grid-cols-3 border-y border-line py-3.5">
              <Spec label="年龄" value={formatAge(pet.age_wsh)} />
              <Spec label="体重" value={formatWeight(pet.weight_wsh)} />
              <Spec
              label="性别"
              value={genderLabel(pet.gender_wsh)}
              muted={Number(pet.gender_wsh) === 0 || pet.gender_wsh == null} />
            
            </dl>

            {/* Health flags */}
            <ul className="flex flex-wrap gap-5">
              <HealthFlag
              active={pet.vaccinated_wsh === 1}
              tone="positive"
              label={pet.vaccinated_wsh === 1 ? '已免疫' : '未免疫'} />
            
              <HealthFlag
              active={pet.sterilized_wsh === 1}
              tone="informative"
              label={pet.sterilized_wsh === 1 ? '已绝育' : '未绝育'} />
            
            </ul>

            {/* Notes */}
            {(pet.description_wsh || pet.allergies_wsh || pet.habits_wsh) &&
          <div className="mt-3 flex-1">
                {pet.description_wsh &&
            <p className="ref-clamp-2 text-[14px] leading-relaxed text-ink-soft">
                    {pet.description_wsh}
                  </p>
            }
                {pet.habits_wsh &&
            <p className="ref-truncate mt-1.5 text-[14px] leading-relaxed text-muted">
                    <span className="text-muted">习惯</span> {pet.habits_wsh}
                  </p>
            }
                {pet.allergies_wsh &&
            <p className="mt-1.5 flex items-start gap-1.5 text-[14px] leading-relaxed text-critical">
                    <AlertTriangleIcon
                className="mt-1 h-3.5 w-3.5 shrink-0"
                aria-hidden="true" />
              
                    <span>
                      <strong className="font-medium">过敏 / 禁忌</strong>{' '}
                      {pet.allergies_wsh}
                    </span>
                  </p>
            }
              </div>
          }

            {/* Actions */}
            <div
            className="mt-4 flex items-center gap-2"
            onClick={(event) => event.stopPropagation()}>
            
              <button
              type="button"
              onClick={() => onView?.(pet.id_wsh)}
              className="rounded-control bg-brand px-3.5 py-2 text-control font-medium text-canvas transition-colors duration-fast ease-editorial hover:bg-brand-deep active:translate-y-px">
              
                查看档案
              </button>
              <button
              type="button"
              onClick={() => onEdit?.(pet)}
              className="rounded-control border border-line bg-surface px-3.5 py-2 text-control font-medium text-ink transition-colors duration-fast ease-editorial hover:border-brand hover:text-brand active:translate-y-px">
              
                编辑
              </button>
              <button
              type="button"
              onClick={() => onDelete?.(pet.id_wsh)}
              aria-label={`删除 ${pet.name_wsh}`}
              className="ml-auto inline-flex items-center gap-1.5 rounded-control px-2.5 py-2 text-control text-muted transition-colors duration-fast ease-editorial hover:bg-critical/[0.06] hover:text-critical">
              
                <Trash2Icon className="h-3.5 w-3.5" aria-hidden="true" />
                删除
              </button>
            </div>
          </article>
        </li>
      )}
    </ul>);

}

/* -------------------------------------------------------------------------
 * Internals
 * ---------------------------------------------------------------------- */

interface SpecProps {
  label: string;
  value: string | null;
  muted?: boolean;
}

function Spec({ label, value, muted = false }: SpecProps) {
  const isEmpty = muted || !value;
  return (
    <div className="border-r border-line px-4 first:pl-0 last:border-r-0 last:pr-0">
      <dt className="text-label font-medium uppercase text-muted">{label}</dt>
      <dd
        data-numeric="true"
        className={`mt-1 text-[14px] font-semibold ${
        isEmpty ? 'text-muted' : 'text-ink'}`
        }>
        
        {value || '—'}
      </dd>
    </div>);

}

interface HealthFlagProps {
  active: boolean;
  tone: 'positive' | 'informative';
  label: string;
}

function HealthFlag({ active, tone, label }: HealthFlagProps) {
  const dot = !active ?
  'bg-line' :
  tone === 'positive' ?
  'bg-positive' :
  'bg-informative';
  return (
    <li
      className={`inline-flex items-center gap-2 text-[14px] ${
      active ? 'text-ink' : 'text-muted'}`
      }>
      
      <span className={`h-1.5 w-1.5 rounded-full ${dot}`} aria-hidden="true" />
      {label}
    </li>);

}

function PetCardSkeleton() {
  return (
    <div className="flex h-full flex-col rounded-card border border-line bg-surface p-5">
      <div className="flex items-start gap-3.5">
        <div className="ref-shimmer h-14 w-14 rounded-full" />
        <div className="min-w-0 flex-1 space-y-2">
          <div className="ref-shimmer h-4 w-1/2 rounded-tile" />
          <div className="ref-shimmer h-3 w-3/4 rounded-tile" />
        </div>
      </div>
      <div className="my-4 grid grid-cols-3 gap-4 border-y border-line py-3.5">
        {[0, 1, 2].map((key) =>
        <div key={key} className="space-y-2">
            <div className="ref-shimmer h-2.5 w-10 rounded-tile" />
            <div className="ref-shimmer h-3.5 w-14 rounded-tile" />
          </div>
        )}
      </div>
      <div className="ref-shimmer h-3 w-full rounded-tile" />
      <div className="ref-shimmer mt-2 h-3 w-4/5 rounded-tile" />
      <div className="mt-4 flex gap-2">
        <div className="ref-shimmer h-9 w-24 rounded-control" />
        <div className="ref-shimmer h-9 w-16 rounded-control" />
      </div>
    </div>);

}

/** Species glyphs kept from the source markup (dog / cat / rabbit / paw). */
function PetTypeGlyph({ type }: {type?: PetType;}) {
  const normalized = normalizeType(type);
  const common = {
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.5,
    className: 'h-6 w-6',
    'aria-hidden': true
  } as const;

  if (normalized === 'dog') {
    return (
      <svg {...common}>
        <path d="M10 5.172C10 3.782 8.423 2.679 6.5 3c-2.823.47-4.113 6.006-4 7 .08.703 1.725 1.722 3.656 1 1.261-.472 1.96-1.45 2.344-2.5M14.267 5.172c0-1.39 1.577-2.493 3.5-2.172 2.823.47 4.113 6.006 4 7-.08.703-1.725 1.722-3.656 1-1.261-.472-1.855-1.45-2.239-2.5M8 14v.5M15.5 14v.5M11.75 16.25c.75.5 2 .5 2.75 0M4.42 11.247A13.152 13.152 0 0 0 4 14.556C4 18.728 7.582 21 12 21s8-2.272 8-6.444c0-1.061-.162-2.2-.493-3.309m-9.243-6.082A8.801 8.801 0 0 1 12 5c.78 0 1.5.108 2.161.306" />
      </svg>);

  }
  if (normalized === 'cat') {
    return (
      <svg {...common}>
        <path d="M12 5c-1.5-3-5-3-5-3s0 3.5 1.5 5M12 5c1.5-3 5-3 5-3s0 3.5-1.5 5M12 5v14M8 19c-2 0-4-1-4-3 0-2 2-3 4-3M16 19c2 0 4-1 4-3 0-2-2-3-4-3M8 19h8" />
      </svg>);

  }
  if (normalized === 'rabbit') {
    return (
      <svg {...common}>
        <path d="M8 2c0 2-2 6-2 10 0 3 2 6 6 6s6-3 6-6c0-4-2-8-2-10M12 18v4M9 22h6" />
      </svg>);

  }
  return (
    <svg {...common}>
      <path d="M11.5 9C11.5 9 9 7 7 9s-2 5 2 7 7-2 7-2-1.5-2-4.5-4z" />
      <circle cx="6.5" cy="6.5" r="1.5" />
      <circle cx="17.5" cy="6.5" r="1.5" />
      <circle cx="11.5" cy="5.5" r="1.5" />
      <path d="M12 18c-4 0-6-2-6-5 0-2 2-4 6-4s6 2 6 4c0 3-2 5-6 5z" />
    </svg>);

}

/* -------------------------------------------------------------------------
 * Helpers — ported verbatim from the source script block
 * ---------------------------------------------------------------------- */

const typeMap: Record<string, string> = {
  dog: '狗',
  cat: '猫',
  rabbit: '兔子',
  bird: '鸟',
  fish: '鱼',
  hamster: '仓鼠',
  other: '其他'
};

export function normalizeType(type?: PetType): string {
  return String(type || 'other').toLowerCase();
}

export function petTypeLabel(type?: PetType): string {
  return typeMap[normalizeType(type)] || type || '其他';
}

export function genderLabel(gender?: number | null): string {
  const value = Number(gender);
  return value === 1 ? '公' : value === 2 ? '母' : '未知';
}

export function formatAge(age?: number | string | null): string | null {
  return age ? `${age} 个月` : null;
}

export function formatWeight(weight?: number | string | null): string | null {
  return weight ? `${weight} kg` : null;
}

export function profileComplete(pet: Pet): boolean {
  return Boolean(pet.description_wsh);
}