const fs = require('fs');
const path = require('path');

const srcDir = path.join(__dirname, 'src');
const excludeDirs = ['__tests__', 'node_modules', 'assets'];

const entityVars = [
  'row', 'a', 'c', 'm', 'k', 'p', 'r', 'o', 's', 't', 'f', 'n', 'pet', 'log', 'banner', 'cat',
  'merchant', 'keeper', 'user', 'order', 'payment', 'rating', 'ticket', 'complaint', 'notice',
  'myMerchant', 'keeperInfo', 'detailPet', 'editingRole', 'editingBanner', 'editingCategory', 'editingPet',
  'selectedTicket', 'certForm', 'profileForm', 'recordForm', 'petForm', 'stats', 'address',
  'form', 'profile', 'item', 'creditInfo', 'role',
  'editing', 'merchantU', 'keeperU', 'ownerU', 'admin', 'merchantUser', 'keeperUser',
  'owner', 'petInfo', 'orderDetail',
];

const fields = [
  'id', 'name', 'nickname', 'username', 'password', 'description', 'avatar', 'breed', 'rating',
  'allergies', 'habits', 'sterilized', 'vaccinated', 'gender', 'weight', 'email_wsh', 'phone', 'bio',
  'latitude', 'longitude', 'price', 'amount', 'title', 'content', 'result', 'reply', 'count',
  'pages', 'unit', 'type', 'experience', 'label', 'remark', 'score', 'days', 'method', 'category',
  'priority', 'images', 'status', 'address', 'sort_order', 'max_pets', 'current_pets',
  'completion_rate', 'complaint_rate', 'price_per_day', 'order_no', 'pay_no', 'final_amount',
  'total_amount', 'discount', 'start_date', 'end_date', 'owner_id', 'role_id', 'merchant_id',
  'keeper_id', 'pet_id', 'parent_id', 'service_id', 'order_id', 'target_id', 'assignee_id',
  'source_type', 'target_type', 'from_user_id', 'to_user_id', 'balance', 'frozen_amount',
  'detail', 'is_default', 'link_url', 'image_url', 'source_path', 'word_count',
  'record_time', 'paid_at', 'reply_at', 'created_at', 'updated_at',
  'reason', 'level', 'species', 'age', 'create_time', 'owner_name', 'user_name',


];

function walk(dir) {
  const files = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!excludeDirs.includes(entry.name)) files.push(...walk(fullPath));
    } else if (entry.isFile() && (entry.name.endsWith('.vue') || entry.name.endsWith('.js'))) {
      files.push(fullPath);
    }
  }
  return files;
}

const files = walk(srcDir);
console.log(`Scanning ${files.length} files...`);
let changed = 0;

for (const file of files) {
  let content = fs.readFileSync(file, 'utf-8');
  let original = content;

  for (const v of entityVars) {
    for (const field of fields) {
      const re = new RegExp(v + '\\.' + field + '(?!_wsh)', 'g');
      content = content.replace(re, v + '.' + field + '_wsh');
    }
  }

  if (content !== original) {
    fs.writeFileSync(file, content, 'utf-8');
    changed++;
    console.log(`  ${path.relative(__dirname, file)}`);
  }
}

console.log(`\nDone! ${changed} files updated.`);
