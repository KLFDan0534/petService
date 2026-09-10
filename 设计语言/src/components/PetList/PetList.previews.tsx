import { PetList } from './index';
import type { Pet } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const pets: Pet[] = [
{
  id_wsh: 1,
  name_wsh: '豆豆',
  type_wsh: 'dog',
  breed_wsh: '柯基',
  age_wsh: 26,
  weight_wsh: 11.4,
  gender_wsh: 1,
  vaccinated_wsh: 1,
  sterilized_wsh: 1,
  description_wsh: '性格温顺，怕打雷，出门必须牵引绳，见到陌生犬会兴奋。',
  habits_wsh: '每天早晚各一次散步，晚饭后需要梳毛',
  allergies_wsh: '鸡肉、部分谷物'
},
{
  id_wsh: 2,
  name_wsh: '奶糖',
  type_wsh: 'cat',
  breed_wsh: '英短蓝白',
  age_wsh: 14,
  weight_wsh: 4.2,
  gender_wsh: 2,
  vaccinated_wsh: 1,
  sterilized_wsh: 0,
  description_wsh: '认生，寄养时需要单独房间，喜欢躲在纸箱里。'
},
{
  id_wsh: 3,
  name_wsh: '小白',
  type_wsh: 'rabbit',
  breed_wsh: null,
  age_wsh: null,
  weight_wsh: null,
  gender_wsh: 0,
  vaccinated_wsh: 0,
  sterilized_wsh: 0
}];


const previews: ComponentPreviewModule = {
  componentName: 'PetList',
  importPath: 'components/PetList',
  previews: [
  {
    name: 'Default',
    description: '三只宠物：完整档案、部分档案与待完善档案',
    render: () =>
    <PetList
      pets={pets}
      onView={(id) => console.log('view', id)}
      onEdit={(pet) => console.log('edit', pet)}
      onDelete={(id) => console.log('delete', id)} />


  },
  {
    name: 'Single pet',
    description: '单只宠物，含过敏与习惯备注',
    render: () => <PetList pets={[pets[0]]} />
  },
  {
    name: 'Loading',
    description: '加载骨架屏，避免空白区域',
    render: () => <PetList pets={[]} loading />
  },
  {
    name: 'Empty',
    description: '空档案状态，可传入 emptyAction 作为主操作',
    render: () =>
    <PetList
      pets={[]}
      emptyAction={
      <button
        type="button"
        className="rounded-control bg-brand px-4 py-2 text-control font-medium text-canvas transition-colors duration-fast ease-editorial hover:bg-brand-deep">
        
              添加宠物
            </button>
      } />


  }]

};

export default previews;