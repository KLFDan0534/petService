# PetList

Pet roster grid for the user 宠物档案 views. Ported from `src/components/pet/PetList.vue` — same card structure (avatar + name, 3-up specs strip, health flags, notes, actions), same props and events, restyled onto the Editorial Warm tokens.

Grid: 1 column, 2 at `md`, 3 at `xl`. Each card is fully clickable (`onView`) with inner buttons stopping propagation.

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `pets` | `Pet[]` | required | Roster. Field names keep the API's `*_wsh` suffixes. |
| `onView` | `(id) => void` | – | Card click, pet name button, and 查看档案. |
| `onEdit` | `(pet: Pet) => void` | – | 编辑 — receives the full record. |
| `onDelete` | `(id) => void` | – | 删除. |
| `loading` | `boolean` | `false` | Renders 3 shimmer skeleton cards instead of the grid. |
| `emptyAction` | `ReactNode` | – | Slot inside the empty state (e.g. an “添加宠物” button). |
| `className` | `string` | `''` | Extra classes on the root grid/panel. |

## Pet shape

```ts
interface Pet {
  id_wsh: string | number
  name_wsh: string
  type_wsh?: 'dog' | 'cat' | 'rabbit' | 'bird' | 'fish' | 'hamster' | 'other' | string
  breed_wsh?: string | null
  avatar_wsh?: string | null
  age_wsh?: number | string | null      // months → “26 个月”
  weight_wsh?: number | string | null   // kg   → “11.4 kg”
  gender_wsh?: number | null            // 0 未知 · 1 公 · 2 母
  vaccinated_wsh?: number | null        // 1 = 已免疫
  sterilized_wsh?: number | null        // 1 = 已绝育
  description_wsh?: string | null       // absence ⇒ “档案待完善” badge
  habits_wsh?: string | null
  allergies_wsh?: string | null
}
```

## Behaviour

- Missing `avatar_wsh` falls back to a species glyph (dog / cat / rabbit / paw) on a `bg-sand` disc.
- Missing `description_wsh` shows the brand-tinted `档案待完善` badge.
- Empty age / weight / unknown gender render `—` in `text-muted`; numeric cells use `data-numeric="true"`.
- Vaccination and sterilization render as dot flags — `positive` / `informative` when set, `line` when not.
- `allergies_wsh` is always surfaced in `text-critical` with a warning icon.
- Hover: `-4px` lift, `shadow-lift`, brand border, and the trailing arrow fades in. Nothing at rest.

## Usage

```tsx
import { PetList } from 'components/PetList'

<PetList
  pets={pets}
  loading={isLoading}
  onView={id => navigate(`/pets/${id}`)}
  onEdit={pet => openPetForm(pet)}
  onDelete={id => confirmDelete(id)}
  emptyAction={<Button onClick={openPetForm}>添加宠物</Button>}
/>
```

## Exported helpers

`petTypeLabel`, `genderLabel`, `formatAge`, `formatWeight`, `normalizeType`, `profileComplete` — kept from the source so detail views format pet fields identically.
