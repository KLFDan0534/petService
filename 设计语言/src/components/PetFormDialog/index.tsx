import { useCallback, useEffect, useRef, useState } from 'react';
import {
  CheckIcon,
  LoaderCircleIcon,
  PawPrintIcon,
  UploadIcon,
  XIcon } from
'lucide-react';

/* ------------------------------------------------------------------ types */

export type PetGenderValue = 0 | 1 | 2;
export type PetFlagValue = 0 | 1;

/** Shape of a pet record coming back from the API (`*_wsh` fields). */
export interface Pet {
  id_wsh?: number | string;
  name_wsh?: string;
  type_wsh?: string;
  breed_wsh?: string | null;
  age_wsh?: number | null;
  weight_wsh?: number | null;
  gender_wsh?: PetGenderValue | null;
  sterilized_wsh?: PetFlagValue | null;
  vaccinated_wsh?: PetFlagValue | null;
  avatar_wsh?: string | null;
  description_wsh?: string | null;
  allergies_wsh?: string | null;
  habits_wsh?: string | null;
}

/** Internal, always-defined form state. */
export interface PetFormValues {
  name_wsh: string;
  type_wsh: string;
  breed_wsh: string;
  age_wsh: number | null;
  weight_wsh: number | null;
  gender_wsh: PetGenderValue;
  sterilized_wsh: PetFlagValue;
  vaccinated_wsh: PetFlagValue;
  avatar_wsh: string;
  description_wsh: string;
  allergies_wsh: string;
  habits_wsh: string;
}

/** Payload emitted on save — empty strings are normalised to `null`. */
export interface PetFormPayload {
  name_wsh: string;
  type_wsh: string;
  breed_wsh: string | null;
  age_wsh: number | null;
  weight_wsh: number | null;
  gender_wsh: number;
  sterilized_wsh: PetFlagValue;
  vaccinated_wsh: PetFlagValue;
  avatar_wsh: string | null;
  description_wsh: string | null;
  allergies_wsh: string | null;
  habits_wsh: string | null;
}

export interface PetFormDialogProps {
  /** Controls dialog visibility. */
  visible?: boolean;
  /** Pet being edited. `null` / omitted puts the dialog in create mode. */
  pet?: Pet | null;
  /** Disables the footer actions and shows the saving label. */
  saving?: boolean;
  /** Server-side error message, surfaced in the form error banner. */
  serverError?: string;
  /** Called with the normalised payload when the form is submitted. */
  onSave?: (payload: PetFormPayload) => void;
  /** Called on scrim click, close button and Escape. */
  onClose?: () => void;
  /**
   * Uploads the chosen avatar file and resolves with its public URL.
   * When omitted, the file is previewed locally without an upload.
   */
  onUploadAvatar?: (file: File) => Promise<string>;
  /** Optional toast hook, used for avatar upload failures. */
  onToast?: (message: string, type: 'error' | 'success') => void;
}

/* ------------------------------------------------------------------ config */

export const petTypeOptions: ReadonlyArray<{value: string;label: string;}> = [
{ value: 'dog', label: '狗' },
{ value: 'cat', label: '猫' },
{ value: 'rabbit', label: '兔子' },
{ value: 'bird', label: '鸟' },
{ value: 'fish', label: '鱼' },
{ value: 'hamster', label: '仓鼠' },
{ value: 'other', label: '其他' }];


/* ------------------------------------------------------------------ styles */

const labelClass = 'mb-1.5 text-[13px] font-medium text-ink';
const controlClass =
'w-full border border-line bg-surface text-[14px] text-ink placeholder:text-muted transition-[border-color,box-shadow] duration-fast ease-editorial focus:border-brand focus:outline-none focus:shadow-focus disabled:opacity-60';
const inputClass = `h-10 rounded-control px-3 ${controlClass}`;
const textareaClass = `resize-none rounded-control px-3 py-2.5 leading-relaxed ${controlClass}`;
const rowClass = 'grid grid-cols-1 gap-4 sm:grid-cols-2';
const groupClass = 'flex min-w-0 flex-col';
const btnBase =
'inline-flex min-h-10 items-center justify-center gap-2 whitespace-nowrap rounded-control px-4 py-2 text-control font-semibold transition-colors duration-fast ease-editorial disabled:cursor-not-allowed disabled:opacity-60';
const btnOutline = `${btnBase} border border-line bg-surface text-ink hover:border-brand hover:bg-sand hover:text-brand`;
const btnPrimary = `${btnBase} bg-brand text-white hover:bg-brand-deep active:translate-y-px`;
const btnQuiet = `${btnBase} border border-transparent text-muted hover:bg-critical/10 hover:text-critical`;

/* ------------------------------------------------------------- component */

export function PetFormDialog({
  visible = false,
  pet = null,
  saving = false,
  serverError = '',
  onSave,
  onClose,
  onUploadAvatar,
  onToast
}: PetFormDialogProps) {
  const [petForm, setPetForm] = useState<PetFormValues>(createEmptyPetForm);
  const [formError, setFormError] = useState('');
  const [avatarPreview, setAvatarPreview] = useState('');
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [avatarBroken, setAvatarBroken] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const patch = useCallback((next: Partial<PetFormValues>) => {
    setPetForm((prev) => ({ ...prev, ...next }));
  }, []);

  // Reset on close, hydrate on open — mirrors the source's visible/pet watcher.
  useEffect(() => {
    if (!visible) {
      setPetForm(createEmptyPetForm());
      setFormError('');
      setAvatarPreview('');
      setAvatarBroken(false);
      return;
    }
    if (pet) {
      setPetForm(populateForm(pet));
      setAvatarPreview(pet.avatar_wsh || '');
    } else {
      setPetForm(createEmptyPetForm());
      setAvatarPreview('');
    }
    setFormError('');
    setAvatarBroken(false);
  }, [visible, pet]);

  useEffect(() => {
    if (serverError) setFormError(serverError);
  }, [serverError]);

  useEffect(() => {
    if (!visible) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !saving) onClose?.();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [visible, saving, onClose]);

  if (!visible) return null;

  const handleAvatarChange = async (
  event: React.ChangeEvent<HTMLInputElement>) =>
  {
    const file = event.target.files?.[0];
    if (!file) return;
    setAvatarUploading(true);
    setAvatarBroken(false);
    try {
      if (onUploadAvatar) {
        const url = await onUploadAvatar(file);
        if (!url) throw new Error('未知错误');
        patch({ avatar_wsh: url });
        setAvatarPreview(url);
      } else {
        const localUrl = await readFileAsDataUrl(file);
        patch({ avatar_wsh: localUrl });
        setAvatarPreview(localUrl);
      }
    } catch (err) {
      const message = '上传失败: ' + ((err as Error)?.message || '未知错误');
      if (onToast) onToast(message, 'error');else
      setFormError(message);
    } finally {
      setAvatarUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const removeAvatar = () => {
    patch({ avatar_wsh: '' });
    setAvatarPreview('');
    setAvatarBroken(false);
  };

  const savePet = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!petForm.name_wsh.trim()) {
      setFormError('请输入宠物名称');
      return;
    }
    setFormError('');
    onSave?.(toPayload(petForm));
  };

  return (
    <div
      className="fixed inset-0 z-[1000] flex items-end justify-center bg-black/50 p-0 backdrop-blur-[4px] animate-fade sm:items-center sm:p-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !saving) onClose?.();
      }}>
      
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="pf-title"
        className="flex max-h-[92vh] w-full max-w-[640px] flex-col overflow-y-auto rounded-t-frame bg-surface p-6 shadow-lift animate-pop sm:rounded-card sm:p-8">
        
        <header className="mb-6 flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h2 id="pf-title" className="font-display text-display-xs text-ink">
              {pet ? '编辑宠物' : '添加宠物'}
            </h2>
            <p className="mt-1 text-meta text-muted">
              {pet ?
              `更新「${pet.name_wsh || ''}」的档案信息` :
              '填写基础信息，之后可随时补充'}
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-tile text-muted transition-colors duration-fast ease-editorial hover:bg-sand hover:text-ink"
            onClick={() => onClose?.()}
            aria-label="关闭">
            
            <XIcon className="h-4 w-4" aria-hidden="true" />
          </button>
        </header>

        <form id="pf-form" onSubmit={savePet} className="flex flex-col gap-5">
          {formError ?
          <div
            className="rounded-control border border-critical/20 bg-critical/[0.06] px-3.5 py-2.5 text-[14px] text-critical"
            role="alert">
            
              {formError}
            </div> :
          null}

          {/* Name + Type */}
          <div className={rowClass}>
            <div className={groupClass}>
              <label className={labelClass} htmlFor="pf-name">
                宠物名称 <span className="text-brand">*</span>
              </label>
              <input
                id="pf-name"
                value={petForm.name_wsh}
                onChange={(e) => patch({ name_wsh: e.target.value })}
                onBlur={(e) => patch({ name_wsh: e.target.value.trim() })}
                placeholder="请输入宠物名称"
                required
                className={inputClass} />
              
            </div>
            <div className={groupClass}>
              <label className={labelClass} htmlFor="pf-type">
                类型
              </label>
              <select
                id="pf-type"
                value={petForm.type_wsh}
                onChange={(e) => patch({ type_wsh: e.target.value })}
                className={inputClass}>
                
                {petTypeOptions.map((option) =>
                <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                )}
              </select>
            </div>
          </div>

          {/* Breed + Gender */}
          <div className={rowClass}>
            <div className={groupClass}>
              <label className={labelClass} htmlFor="pf-breed">
                品种
              </label>
              <input
                id="pf-breed"
                value={petForm.breed_wsh}
                onChange={(e) => patch({ breed_wsh: e.target.value })}
                onBlur={(e) => patch({ breed_wsh: e.target.value.trim() })}
                placeholder="如：金毛、英短"
                className={inputClass} />
              
            </div>
            <div className={groupClass}>
              <label className={labelClass} htmlFor="pf-gender">
                性别
              </label>
              <select
                id="pf-gender"
                value={petForm.gender_wsh}
                onChange={(e) =>
                patch({ gender_wsh: Number(e.target.value) as PetGenderValue })
                }
                className={inputClass}>
                
                <option value={0}>未知</option>
                <option value={1}>公</option>
                <option value={2}>母</option>
              </select>
            </div>
          </div>

          {/* Age + Weight */}
          <div className={rowClass}>
            <div className={groupClass}>
              <label className={labelClass} htmlFor="pf-age">
                年龄（月）
              </label>
              <input
                id="pf-age"
                value={petForm.age_wsh ?? ''}
                onChange={(e) => patch({ age_wsh: toNumberOrNull(e.target.value) })}
                type="number"
                min="1"
                placeholder="如：12"
                data-numeric="true"
                className={inputClass} />
              
            </div>
            <div className={groupClass}>
              <label className={labelClass} htmlFor="pf-weight">
                体重（kg）
              </label>
              <input
                id="pf-weight"
                value={petForm.weight_wsh ?? ''}
                onChange={(e) =>
                patch({ weight_wsh: toNumberOrNull(e.target.value) })
                }
                type="number"
                min="0.1"
                step="0.1"
                placeholder="如：5.5"
                data-numeric="true"
                className={inputClass} />
              
            </div>
          </div>

          {/* Avatar upload */}
          <div className={groupClass}>
            <span className={labelClass}>头像</span>
            <div className="flex flex-wrap items-center gap-4 rounded-card border border-line bg-sand p-3">
              <div className="relative h-16 w-16 shrink-0">
                {avatarPreview && !avatarBroken ?
                <img
                  className="h-16 w-16 rounded-full border-2 border-line object-cover"
                  src={avatarPreview}
                  alt="头像预览"
                  onError={() => setAvatarBroken(true)} /> :


                <div className="flex h-16 w-16 items-center justify-center rounded-full border-2 border-dashed border-line text-muted">
                    <PawPrintIcon className="h-6 w-6" aria-hidden="true" />
                  </div>
                }
                {avatarUploading ?
                <span className="absolute inset-0 flex items-center justify-center rounded-full bg-surface/75">
                    <LoaderCircleIcon
                    className="h-4 w-4 animate-spin text-muted"
                    aria-hidden="true" />
                  
                    <span className="sr-only">上传中</span>
                  </span> :
                null}
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <label
                  className={`${btnOutline} cursor-pointer`}
                  tabIndex={0}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      fileInputRef.current?.click();
                    }
                  }}>
                  
                  <UploadIcon className="h-3.5 w-3.5" aria-hidden="true" />
                  选择照片
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={handleAvatarChange} />
                  
                </label>
                {avatarPreview && !avatarUploading ?
                <button type="button" className={btnQuiet} onClick={removeAvatar}>
                    <XIcon className="h-3.5 w-3.5" aria-hidden="true" />
                    移除
                  </button> :
                null}
              </div>

              <p className="w-full text-[12px] text-muted">
                建议正方形图片，JPG / PNG，5MB 以内
              </p>
            </div>
          </div>

          {/* Check cards */}
          <div className={rowClass}>
            <CheckCard
              label="已完成疫苗 / 免疫"
              checked={petForm.vaccinated_wsh === 1}
              onChange={(checked) => patch({ vaccinated_wsh: checked ? 1 : 0 })} />
            
            <CheckCard
              label="已绝育"
              checked={petForm.sterilized_wsh === 1}
              onChange={(checked) => patch({ sterilized_wsh: checked ? 1 : 0 })} />
            
          </div>

          {/* Description */}
          <div className={groupClass}>
            <label className={labelClass} htmlFor="pf-desc">
              性格与照护说明
            </label>
            <textarea
              id="pf-desc"
              value={petForm.description_wsh}
              onChange={(e) => patch({ description_wsh: e.target.value })}
              onBlur={(e) => patch({ description_wsh: e.target.value.trim() })}
              rows={3}
              placeholder="如：胆小、亲人、需要慢慢适应新环境"
              className={textareaClass} />
            
          </div>

          {/* Allergies + Habits */}
          <div className={rowClass}>
            <div className={groupClass}>
              <label className={labelClass} htmlFor="pf-allergies">
                过敏 / 禁忌
              </label>
              <textarea
                id="pf-allergies"
                value={petForm.allergies_wsh}
                onChange={(e) => patch({ allergies_wsh: e.target.value })}
                onBlur={(e) => patch({ allergies_wsh: e.target.value.trim() })}
                rows={3}
                placeholder="如：鸡肉过敏、不能吃牛奶"
                className={textareaClass} />
              
            </div>
            <div className={groupClass}>
              <label className={labelClass} htmlFor="pf-habits">
                生活习惯
              </label>
              <textarea
                id="pf-habits"
                value={petForm.habits_wsh}
                onChange={(e) => patch({ habits_wsh: e.target.value })}
                onBlur={(e) => patch({ habits_wsh: e.target.value.trim() })}
                rows={3}
                placeholder="如：每天早晚各遛一次"
                className={textareaClass} />
              
            </div>
          </div>
        </form>

        <footer className="mt-6 flex items-center justify-end gap-2 border-t border-line pt-4">
          <button
            type="button"
            className={btnOutline}
            onClick={() => onClose?.()}
            disabled={saving}>
            
            取消
          </button>
          <button
            type="submit"
            form="pf-form"
            className={btnPrimary}
            disabled={saving}>
            
            {saving ? '保存中...' : pet ? '保存' : '添加'}
          </button>
        </footer>
      </div>
    </div>);

}

/* --------------------------------------------------------------- internals */

interface CheckCardProps {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}

function CheckCard({ label, checked, onChange }: CheckCardProps) {
  return (
    <label
      className={[
      'flex cursor-pointer items-center gap-2.5 rounded-control border px-3.5 py-2.5 text-[14px] font-medium transition-colors duration-fast ease-editorial',
      checked ?
      'border-brand bg-brand/[0.06] text-ink' :
      'border-line bg-surface text-muted hover:border-brand/40'].
      join(' ')}>
      
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="sr-only" />
      
      <span
        aria-hidden="true"
        className={[
        'inline-flex h-[18px] w-[18px] shrink-0 items-center justify-center rounded border-2 transition-colors duration-fast ease-editorial',
        checked ?
        'border-brand bg-brand text-white' :
        'border-line bg-surface'].
        join(' ')}>
        
        {checked ? <CheckIcon className="h-3 w-3" strokeWidth={3} /> : null}
      </span>
      {label}
    </label>);

}

export function createEmptyPetForm(): PetFormValues {
  return {
    name_wsh: '',
    type_wsh: 'dog',
    breed_wsh: '',
    age_wsh: null,
    weight_wsh: null,
    gender_wsh: 0,
    sterilized_wsh: 0,
    vaccinated_wsh: 0,
    avatar_wsh: '',
    description_wsh: '',
    allergies_wsh: '',
    habits_wsh: ''
  };
}

function normalizeType(type?: string | null): string {
  return String(type || 'other').toLowerCase();
}

function emptyToNull<T>(value: T): T | null {
  return value === '' || value === undefined ? null : value;
}

function toNumberOrNull(value: string): number | null {
  if (value === '') return null;
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}

function populateForm(pet: Pet): PetFormValues {
  return {
    name_wsh: pet.name_wsh || '',
    type_wsh: normalizeType(pet.type_wsh),
    breed_wsh: pet.breed_wsh || '',
    age_wsh: pet.age_wsh ?? null,
    weight_wsh: pet.weight_wsh ?? null,
    gender_wsh: (pet.gender_wsh ?? 0) as PetGenderValue,
    sterilized_wsh: (pet.sterilized_wsh ?? 0) as PetFlagValue,
    vaccinated_wsh: (pet.vaccinated_wsh ?? 0) as PetFlagValue,
    avatar_wsh: pet.avatar_wsh || '',
    description_wsh: pet.description_wsh || '',
    allergies_wsh: pet.allergies_wsh || '',
    habits_wsh: pet.habits_wsh || ''
  };
}

export function toPayload(form: PetFormValues): PetFormPayload {
  return {
    name_wsh: form.name_wsh,
    type_wsh: form.type_wsh,
    breed_wsh: emptyToNull(form.breed_wsh),
    age_wsh: emptyToNull(form.age_wsh),
    weight_wsh: emptyToNull(form.weight_wsh),
    gender_wsh: Number(form.gender_wsh) || 0,
    sterilized_wsh: form.sterilized_wsh ? 1 : 0,
    vaccinated_wsh: form.vaccinated_wsh ? 1 : 0,
    avatar_wsh: emptyToNull(form.avatar_wsh),
    description_wsh: emptyToNull(form.description_wsh),
    allergies_wsh: emptyToNull(form.allergies_wsh),
    habits_wsh: emptyToNull(form.habits_wsh)
  };
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(new Error('读取文件失败'));
    reader.readAsDataURL(file);
  });
}