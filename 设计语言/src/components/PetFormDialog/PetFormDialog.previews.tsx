import { useState } from 'react';
import { PetFormDialog, type Pet, type PetFormPayload } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const samplePet: Pet = {
  id_wsh: 12,
  name_wsh: '豆豆',
  type_wsh: 'DOG',
  breed_wsh: '金毛',
  age_wsh: 26,
  weight_wsh: 18.4,
  gender_wsh: 1,
  sterilized_wsh: 1,
  vaccinated_wsh: 1,
  avatar_wsh: '',
  description_wsh: '亲人，喜欢被摸头，第一次见陌生人会先躲一下。',
  allergies_wsh: '鸡肉过敏',
  habits_wsh: '每天早晚各遛一次，晚饭后散步 30 分钟。'
};

function Frame({ children }: {children: React.ReactNode;}) {
  return (
    <div className="relative min-h-[620px] w-full overflow-hidden bg-canvas">
      {children}
    </div>);

}

function CreateDemo() {
  const [saving, setSaving] = useState(false);
  const [pet, setPet] = useState<PetFormPayload | null>(null);

  const handleSave = (payload: PetFormPayload) => {
    setSaving(true);
    setPet(payload);
    window.setTimeout(() => setSaving(false), 900);
  };

  return (
    <Frame>
      <PetFormDialog visible pet={null} saving={saving} onSave={handleSave} />
      {pet ? <span className="sr-only">saved {pet.name_wsh}</span> : null}
    </Frame>);

}

const previews: ComponentPreviewModule = {
  componentName: 'PetFormDialog',
  importPath: 'components/PetFormDialog',
  previews: [
  {
    name: 'Create',
    description: '添加宠物 — empty form, name required, avatar previewed locally.',
    render: () => <CreateDemo />
  },
  {
    name: 'Edit',
    description: '编辑宠物 — hydrated from an existing pet record.',
    render: () =>
    <Frame>
          <PetFormDialog visible pet={samplePet} />
        </Frame>

  },
  {
    name: 'Saving',
    description: 'Footer actions disabled while the payload is in flight.',
    render: () =>
    <Frame>
          <PetFormDialog visible pet={samplePet} saving />
        </Frame>

  },
  {
    name: 'Server error',
    description: 'Server-side failure surfaced in the form error banner.',
    render: () =>
    <Frame>
          <PetFormDialog
        visible
        pet={null}
        serverError="保存失败：宠物名称已存在，请更换后重试" />
      
        </Frame>

  }]

};

export default previews;