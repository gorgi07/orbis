'use strict';
/* Orbis UI. The REST contract and the domain/business logic remain unchanged. */
const root = document.querySelector('#app');
const mode = root.dataset.mode;
const currentId = Number(root.dataset.id || 0);
const csrf = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
const dialog = document.querySelector('#edit-dialog');
const modalContent = document.querySelector('#modal-content');
const confirmationDialog = document.querySelector('#confirm-dialog');
const E = value => String(value ?? '—').replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
const colors = ['RED', 'BLUE', 'YELLOW'];
const countries = ['USA', 'INDIA', 'ITALY', 'SOUTH_KOREA'];
const colorNames = { RED:'Красный', BLUE:'Синий', YELLOW:'Жёлтый' };
const countryNames = { USA:'США', INDIA:'Индия', ITALY:'Италия', SOUTH_KOREA:'Южная Корея' };
const colorLabel = value => colorNames[value] || 'Не указан';
const countryLabel = value => countryNames[value] || 'Не указана';
const prettyDate = value => value ? new Date(value).toLocaleString('ru-RU', {day:'2-digit',month:'long',year:'numeric',hour:'2-digit',minute:'2-digit'}) : '—';
const prettyNumber = value => value == null ? '—' : Number(value).toLocaleString('ru-RU', {maximumFractionDigits:5});
const dash = '<span class="dash">—</span>';
const symbol = {
 plus:'<path d="M12 5v14M5 12h14"/>',
 sliders:'<path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="2" fill="white"/><circle cx="15" cy="17" r="2" fill="white"/>',
 arrowDown:'<path d="M12 4v16m-6-6 6 6 6-6"/>',
 sort:'<path d="M7 4v16m-3-3 3 3 3-3M17 20V4m-3 3 3-3 3 3"/>',
 chevron:'<path d="m9 18 6-6-6-6"/>',
 back:'<path d="M19 12H5m6-6-6 6 6 6"/>',
 close:'<path d="M18 6 6 18M6 6l12 12"/>',
 pencil:'<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L9 17l-4 1 1-4Z"/>',
 trash:'<path d="M3 6h18M8 6V4h8v2m3 0-1 15H6L5 6m5 4v7m4-7v7"/>',
 pin:'<path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/>',
 users:'<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2m14-18a4 4 0 0 1 0 8m6 10v-2a4 4 0 0 0-3-3.87"/><circle cx="9" cy="7" r="4"/>',
 search:'<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
 shield:'<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"/>',
 clock:'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l4 3"/>',
 alert:'<path d="M12 3 2 20h20L12 3Z"/><path d="M12 9v5m0 3h.01"/>',
 check:'<path d="m5 12 4 4L19 6"/>',
 info:'<circle cx="12" cy="12" r="9"/><path d="M12 11v5m0-8h.01"/>',
 clipboard:'<rect x="5" y="5" width="14" height="17" rx="2"/><path d="M9 5V3h6v2m-6 6h6m-6 4h6"/>',
 chart:'<path d="M4 20h16M7 17v-5m5 5V6m5 11V9"/>',
 eye:'<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/>',
 layers:'<path d="m12 2 9 5-9 5-9-5 9-5Zm-9 10 9 5 9-5m-18 5 9 5 9-5"/>',
 arrowUp:'<path d="M12 20V4m-6 6 6-6 6 6"/>'
};
function icon(name, size=18) { return `<svg class="icon" width="${size}" height="${size}" viewBox="0 0 24 24" aria-hidden="true">${symbol[name]||symbol.info}</svg>`; }
function options(values, selected, nullable=true, labels={}) {
 return `${nullable ? '<option value="">Не указано</option>' : ''}${values.map(v=>`<option value="${E(v)}" ${v===selected?'selected':''}>${E(labels[v] || v)}</option>`).join('')}`;
}
function notify(message, isError=false){
 const toast=document.createElement('div');toast.className=`toast${isError?' err':''}`;toast.textContent=message;
 document.querySelector('#toasts').append(toast);setTimeout(()=>toast.remove(),5400);
}
async function api(path, opts={}) {
 const headers={Accept:'application/json',...(opts.body!==undefined?{'Content-Type':'application/json'}:{}),...(opts.method && opts.method!=='GET'?{[csrfHeader]:csrf}:{}),...(opts.key?{'Idempotency-Key':opts.key}:{})};
 const response=await fetch(path,{method:opts.method||'GET',credentials:'same-origin',headers,body:opts.body===undefined?undefined:JSON.stringify(opts.body)});
 if(response.redirected && response.url.includes('/login')){window.location.href='/login';throw new Error('Требуется войти в аккаунт');}
 const text=await response.text();let data=null;
 try{data=text?JSON.parse(text):null;}catch{data=null;}
 if(!response.ok)throw new Error(data?.error || `Ошибка сервера (${response.status})`);
 return data;
}
function pageHead(title, description='', actions='', eyebrow=''){
 return `<div class="pagehead"><div>${eyebrow?`<div class="eyebrow">${eyebrow}</div>`:''}<h1>${title}</h1>${description?`<p>${description}</p>`:''}</div>${actions?`<div class="pagehead-actions">${actions}</div>`:''}</div>`;
}
function field(label, id, content, hint=''){
 return `<div class="form-field"><label for="${id}">${label}</label>${content}${hint?`<p class="form-hint">${hint}</p>`:''}</div>`;
}
function numberInput(name, value, props='') {return `<input id="${name}" name="${name}" type="number" inputmode="decimal" step="any" ${props} value="${E(value ?? '')}">`;}
function fieldSection(title, body, description=''){
 return `<section class="form-section"><h3 class="form-section-title">${title}</h3>${description?`<p class="form-section-description">${description}</p>`:''}<div class="form-grid">${body}</div></section>`;
}
function modalHead(title, subtitle=''){
 return `<header class="modal-titlebar"><div><h2>${title}</h2>${subtitle?`<p>${subtitle}</p>`:''}</div><button type="button" class="modal-close" data-close-modal aria-label="Закрыть">${icon('close')}</button></header>`;
}
function openModal(markup){modalContent.innerHTML=markup;dialog.showModal();modalContent.querySelectorAll('[data-close-modal]').forEach(button=>button.addEventListener('click',()=>dialog.close()));}
function confirmAction({title='Удалить запись?',description='Это действие нельзя отменить.',button='Удалить'}={}) {
 return new Promise(resolve=>{
  document.getElementById('confirm-heading').textContent=title;
  document.getElementById('confirm-description').textContent=description;
  document.getElementById('confirm-accept').textContent=button;
  const accept=document.getElementById('confirm-accept');const cancel=document.getElementById('confirm-cancel');
  const finish=confirmed=>{confirmationDialog.removeEventListener('cancel',onCancel);accept.removeEventListener('click',onAccept);cancel.removeEventListener('click',onCancel);confirmationDialog.close();resolve(confirmed);};
  const onAccept=()=>finish(true);const onCancel=e=>{if(e)e.preventDefault();finish(false);};
  accept.addEventListener('click',onAccept);cancel.addEventListener('click',onCancel);confirmationDialog.addEventListener('cancel',onCancel);
  confirmationDialog.showModal();
 });
}
// A single native popover for every table row avoids clipping inside horizontal scroll areas.
function openRowActions(button, actions) {
 let popup = document.getElementById('row-actions-popover');
 if (!popup) {
  popup = document.createElement('div');
  popup.id = 'row-actions-popover';
  popup.className = 'row-actions-popover';
  popup.setAttribute('popover', 'auto');
  popup.setAttribute('role', 'menu');
  popup.setAttribute('aria-label', 'Действия с записью');
  document.body.append(popup);
 }
 if (popup.matches(':popover-open')) popup.hidePopover();
 popup.replaceChildren();
 actions.forEach(action => {
  const item = document.createElement('button');
  item.type = 'button';
  item.className = `row-menu-item${action.danger ? ' danger' : ''}`;
  item.setAttribute('role', 'menuitem');
  item.innerHTML = `${icon(action.icon, 16)} <span>${E(action.label)}</span>`;
  item.addEventListener('click', () => { popup.hidePopover(); action.run(); });
  popup.append(item);
 });
 popup.style.left = '0px';
 popup.style.top = '0px';
 popup.showPopover();
 const bounds = button.getBoundingClientRect();
 const width = popup.getBoundingClientRect().width;
 const height = popup.getBoundingClientRect().height;
 const left = Math.min(Math.max(10, bounds.right - width), window.innerWidth - width - 10);
 const below = bounds.bottom + 5;
 const top = below + height > window.innerHeight - 10 ? bounds.top - height - 5 : below;
 popup.style.left = `${left}px`;
 popup.style.top = `${Math.max(10, top)}px`;
 popup.querySelector('button')?.focus();
}
const isInteractiveTarget = element => !!element.closest('a, button, input, select, textarea, summary');
let locations=[];
async function loadLocations(){locations=await api('/api/locations') || [];return locations;}
function locationText(l){return `${l.name || `Локация №${l.id}`} · ${prettyNumber(l.x)}, ${prettyNumber(l.y)}`;}
function personForm(p={}){
 const locationOptions=`<option value="">Не привязывать</option>${locations.map(l=>`<option value="${l.id}" ${p.location?.id===l.id?'selected':''}>${E(locationText(l))}</option>`).join('')}`;
 return `<form id="person-form">
 ${fieldSection('Основная информация',
  field('Имя и фамилия *','name',`<input id="name" name="name" required maxlength="200" placeholder="Например, Александр Иванов" autocomplete="off" value="${E(p.name || '')}">`)+
  field('Гражданство','nationality',`<select id="nationality" name="nationality">${options(countries,p.nationality,true,countryNames)}</select>`, 'Необязательное поле')
 )}
 ${fieldSection('Характеристики',
  field('Рост, см *','height',numberInput('height',p.height??'','min="0.00000001" required placeholder="175"'))+
  field('Вес, кг *','weight',numberInput('weight',p.weight??'','min="0.00000001" required placeholder="70"'))+
  field('Цвет глаз','eyeColor',`<select id="eyeColor" name="eyeColor">${options(colors,p.eyeColor,true,colorNames)}</select>`)+
  field('Цвет волос','hairColor',`<select id="hairColor" name="hairColor">${options(colors,p.hairColor,true,colorNames)}</select>`)
 )}
 ${fieldSection('Координаты',
  field('Координата X *','coordinateX',numberInput('coordinateX',p.coordinateX??0,'step="1" required'))+
  field('Координата Y *','coordinateY',numberInput('coordinateY',p.coordinateY??0,'required'))
 )}
 ${fieldSection('Расположение',
  field('Связанная локация','locationId',`<select id="locationId" name="locationId">${locationOptions}</select>`,'Можно выбрать существующую локацию или оставить без привязки.')
 )}
 <div class="form-actions form-actions-inset"><button class="btn btn-secondary" type="button" id="cancel-edit">Отмена</button><button class="btn btn-primary" type="submit">${icon('check',16)} Сохранить</button></div></form>`;
}
function personData(form,version=null){
 const f=new FormData(form);const optional=name=>f.get(name)||null;
 return {name:String(f.get('name')||'').trim(), coordinateX:Number(f.get('coordinateX')), coordinateY:Number(f.get('coordinateY')),
  height:Number(f.get('height')), weight:Number(f.get('weight')), eyeColor:optional('eyeColor'),hairColor:optional('hairColor'),
  nationality:optional('nationality'),locationId:optional('locationId')?Number(optional('locationId')):null, version};
}
function wirePersonForm(person=null,inModal=false){
 const form=document.querySelector('#person-form');
 document.querySelector('#cancel-edit').onclick=()=>inModal?dialog.close():window.location.assign('/people');
 form.onsubmit=async event=>{
  event.preventDefault();const submit=form.querySelector('[type="submit"]');submit.disabled=true;
  try{
   const body=personData(form,person?.version??null);
   if(person){await api(`/api/people/${person.id}`,{method:'PUT',body});dialog.close();notify('Данные обновлены');await refresh();}
   else {const result=await api('/api/people',{method:'POST',body,key:crypto.randomUUID()});notify('Человек добавлен');window.location.assign(`/people/${result.id}`);}
  }catch(err){notify(err.message,true);}finally{submit.disabled=false;}
 };
}
async function editPerson(personId){
 try{await loadLocations();const person=await api(`/api/people/${personId}`);
  openModal(modalHead('Редактировать данные',`Изменения в записи №${personId}`)+personForm(person));wirePersonForm(person,true);
 }catch(err){notify(err.message,true);}
}
window.editPerson=editPerson;
function colorChip(value){return value?`<span class="chip chip-${value.toLowerCase()}">${E(colorLabel(value))}</span>`:dash;}
function personRow(p){return `<tr class="clickable-row" data-person-row="${p.id}" tabindex="0" aria-label="Открыть карточку: ${E(p.name)}">
 <td class="numeric subtle-id">${p.id}</td>
 <td><a class="person-name" href="/people/${p.id}">${E(p.name)}</a></td>
 <td class="numeric">${prettyNumber(p.coordinateX)}</td><td class="numeric">${prettyNumber(p.coordinateY)}</td>
 <td>${E(prettyDate(p.creationDate))}</td><td>${colorChip(p.eyeColor)}</td><td>${colorChip(p.hairColor)}</td>
 <td>${p.location?E(p.location.name||`Локация №${p.location.id}`):dash}</td>
 <td class="numeric">${p.location?E(p.location.id):dash}</td><td class="numeric">${p.location?prettyNumber(p.location.x):dash}</td><td class="numeric">${p.location?prettyNumber(p.location.y):dash}</td>
 <td class="numeric">${prettyNumber(p.height)} см</td><td class="numeric">${prettyNumber(p.weight)} кг</td>
 <td class="country-label">${p.nationality?E(countryLabel(p.nationality)):dash}</td>
 <td class="row-menu-cell"><button class="row-more-button" type="button" data-person-more="${p.id}" aria-label="Меню для ${E(p.name)}" aria-haspopup="menu" title="Действия с записью">⋯</button></td>
 </tr>`;}
const defaultState=()=>({page:0,name:'',eyeColor:'',hairColor:'',nationality:'',locationName:'',sortBy:'creationDate',direction:'desc'});
let state=defaultState();
const filterLabels={name:'Имя',eyeColor:'Цвет глаз',hairColor:'Цвет волос',nationality:'Гражданство',locationName:'Локация'};
const sortFields=[['creationDate','Дата создания'],['name','Имя'],['id','ID'],['height','Рост'],['weight','Вес'],['eyeColor','Цвет глаз'],['hairColor','Цвет волос'],['nationality','Гражданство'],['coordinateX','Координата X'],['coordinateY','Координата Y'],['locationName','Локация']];
function sortDirections(field){
 if(field==='creationDate')return [['desc','Сначала новые'],['asc','Сначала старые']];
 if(field==='id')return [['desc','Сначала последние'],['asc','Сначала первые']];
 if(field==='height'||field==='weight'||field==='coordinateX'||field==='coordinateY')return [['asc','От меньшего к большему'],['desc','От большего к меньшему']];
 if(field==='name'||field==='locationName')return [['asc','От А до Я'],['desc','От Я до А']];
 return [['asc','По возрастанию'],['desc','По убыванию']];
}
function filterCount(){return Object.keys(filterLabels).filter(k=>state[k]!=='').length;}
function filterPanel(){return `<div id="filters-panel" class="filters-panel" hidden>
 <div class="filter-heading"><h3>Фильтры</h3><p>Укажите точные значения, чтобы отобрать записи.</p></div>
 <form id="filters" class="form-grid">
 ${field('Имя (полностью)','f-name',`<input id="f-name" name="name" placeholder="Точное имя" value="${E(state.name)}">`)}
 ${field('Цвет глаз','f-eyes',`<select id="f-eyes" name="eyeColor">${options(colors,state.eyeColor,true,colorNames)}</select>`)}
 ${field('Цвет волос','f-hair',`<select id="f-hair" name="hairColor">${options(colors,state.hairColor,true,colorNames)}</select>`)}
 ${field('Гражданство','f-nationality',`<select id="f-nationality" name="nationality">${options(countries,state.nationality,true,countryNames)}</select>`)}
 ${field('Название локации (полностью)','f-location',`<input id="f-location" name="locationName" placeholder="Точное название" value="${E(state.locationName)}">`)}
 </form>
 <div class="filter-actions"><button class="btn btn-secondary" id="clear-filters" type="button">Очистить</button><button type="submit" form="filters" class="btn btn-primary">${icon('check',16)} Показать результаты</button></div>
 </div>`;}
function peoplePage(){
 const headers=[['id','ID'],['name','Человек'],['coordinateX','Координата X'],['coordinateY','Координата Y'],['creationDate','Дата добавления'],['eyeColor','Цвет глаз'],['hairColor','Цвет волос'],['','Локация'],['','№ локации'],['','Локация X'],['','Локация Y'],['height','Рост'],['weight','Вес'],['nationality','Гражданство'],['','']];
 return pageHead('Люди','',`<a class="btn btn-primary" href="/people/new">${icon('plus')} Добавить человека</a>`)+
 `<section class="card">
 <div class="list-toolbar"><div class="toolbar-leading"><button class="btn btn-secondary" type="button" id="toggle-filters" aria-expanded="false" aria-controls="filters-panel">${icon('sliders')} Фильтры <span class="count-indicator" id="filter-count" hidden></span></button></div>
 <div class="toolbar-trailing"><div class="sort-control"><label for="f-sort">Сортировка</label><select id="f-sort" aria-label="Сортировать по">${sortFields.map(([v,label])=>`<option value="${v}">${label}</option>`).join('')}</select><select id="f-direction" aria-label="Порядок сортировки"></select></div></div></div>
 ${filterPanel()}<div id="filter-chips" class="filter-chips" hidden></div>
 <div class="table-hint">${icon('info',14)} Таблицу можно прокручивать по горизонтали</div>
 <div class="table-scroll" tabindex="0" role="region" aria-label="Таблица людей; горизонтальная прокрутка"><table class="data-table"><thead><tr>${headers.map(([key,label])=>`<th scope="col" ${!label?'aria-label="Меню строки"':''}>${key?`<button class="table-head-action" type="button" data-sort="${key}" title="Сортировать по полю ${label}">${label} ${icon('sort',13)}</button>`:label}</th>`).join('')}</tr></thead><tbody id="rows"><tr><td colspan="15" class="empty">Загрузка…</td></tr></tbody></table></div>
 <div class="table-footer"><span id="results" class="results-label"></span><div class="pagination"><button id="prev" class="btn btn-small" type="button">Назад</button><span id="page-label" class="pagination-label"></span><button id="next" class="btn btn-small" type="button">Далее ${icon('chevron',14)}</button></div></div>
 </section>`;
}
function renderSortSelectors(){
 const sortSelect=document.getElementById('f-sort');const directionSelect=document.getElementById('f-direction');
 sortSelect.value=state.sortBy;
 directionSelect.innerHTML=sortDirections(state.sortBy).map(([key,label])=>`<option value="${key}">${label}</option>`).join('');
 directionSelect.value=state.direction;
 document.querySelectorAll('[data-sort]').forEach(button=>button.classList.toggle('active',button.dataset.sort===state.sortBy));
}
function filterValue(key,value){if(key==='eyeColor'||key==='hairColor')return colorLabel(value);if(key==='nationality')return countryLabel(value);return value;}
function renderFilterChips(){
 const area=document.getElementById('filter-chips');const count=filterCount();const badge=document.getElementById('filter-count');
 badge.hidden=count===0;badge.textContent=count;
 area.hidden=count===0;
 area.innerHTML=Object.keys(filterLabels).filter(key=>state[key]!=='').map(key=>`<span class="filter-chip">${filterLabels[key]}: ${E(filterValue(key,state[key]))}<button type="button" data-remove-filter="${key}" aria-label="Убрать фильтр ${filterLabels[key]}">${icon('close',13)}</button></span>`).join('') + (count?'<button type="button" class="filter-reset" id="clear-all-filters">Сбросить всё</button>':'');
 area.querySelectorAll('[data-remove-filter]').forEach(button=>button.onclick=()=>{
  state[button.dataset.removeFilter]='';state.page=0;syncFilterInputs();renderFilterChips();renderPeopleRows();
 });
 const reset=document.getElementById('clear-all-filters');if(reset)reset.onclick=clearFilters;
}
function syncFilterInputs(){const form=document.getElementById('filters');if(!form)return;Object.keys(filterLabels).forEach(key=>{const input=form.elements.namedItem(key);if(input)input.value=state[key];});}
function clearFilters(){Object.keys(filterLabels).forEach(k=>state[k]='');state.page=0;syncFilterInputs();renderFilterChips();renderPeopleRows();}
async function listPeople(){
 root.innerHTML=peoplePage();renderSortSelectors();renderFilterChips();
 const panel=document.getElementById('filters-panel');const toggle=document.getElementById('toggle-filters');
 toggle.onclick=()=>{panel.hidden=!panel.hidden;toggle.setAttribute('aria-expanded',String(!panel.hidden));};
 document.getElementById('filters').onsubmit=event=>{
  event.preventDefault();const form=new FormData(event.target);
  Object.keys(filterLabels).forEach(key=>state[key]=String(form.get(key)||'').trim());
  state.page=0;panel.hidden=true;toggle.setAttribute('aria-expanded','false');renderFilterChips();renderPeopleRows();
 };
 document.getElementById('clear-filters').onclick=clearFilters;
 document.getElementById('f-sort').onchange=event=>{
  state.sortBy=event.target.value;state.direction=sortDirections(state.sortBy)[0][0];state.page=0;renderSortSelectors();renderPeopleRows();
 };
 document.getElementById('f-direction').onchange=event=>{state.direction=event.target.value;state.page=0;renderPeopleRows();};
 document.getElementById('prev').onclick=()=>{state.page=Math.max(0,state.page-1);renderPeopleRows();};
 document.getElementById('next').onclick=()=>{state.page++;renderPeopleRows();};
 const peopleRows = document.getElementById('rows');
 peopleRows.addEventListener('click', event => {
  const menuButton = event.target.closest('[data-person-more]');
  if (menuButton) {
   const personId = Number(menuButton.dataset.personMore);
   const row = menuButton.closest('[data-person-row]');
   const personName = row.querySelector('.person-name').textContent;
   openRowActions(menuButton, [
    {icon:'eye', label:'Открыть карточку', run:() => window.location.assign(`/people/${personId}`)},
    {icon:'pencil', label:'Изменить', run:() => editPerson(personId)},
    {icon:'trash', label:'Удалить', danger:true, run:() => deletePersonFromList(personId, personName)}
   ]);
   return;
  }
  const row = event.target.closest('[data-person-row]');
  if (row && !isInteractiveTarget(event.target)) window.location.assign(`/people/${row.dataset.personRow}`);
 });
 peopleRows.addEventListener('keydown', event => {
  if (event.target.matches('[data-person-row]') && (event.key === 'Enter' || event.key === ' ')) {
   event.preventDefault();
   window.location.assign(`/people/${event.target.dataset.personRow}`);
  }
 });
 document.querySelectorAll('[data-sort]').forEach(button=>button.onclick=()=>{
  const field=button.dataset.sort;
  if(field===state.sortBy)state.direction=state.direction==='asc'?'desc':'asc';
  else {state.sortBy=field;state.direction=sortDirections(field)[0][0];}
  state.page=0;renderSortSelectors();renderPeopleRows();
 });
 await renderPeopleRows();
}
async function deletePersonFromList(id, name){
 if (!await confirmAction({title:'Удалить человека?', description:`Запись «${name}» будет удалена без возможности восстановления.`})) return;
 try { await api(`/api/people/${id}`, {method:'DELETE'}); notify('Человек удалён'); await renderPeopleRows(); }
 catch(err){notify(err.message,true);}
}
async function renderPeopleRows(){
 const tbody=document.getElementById('rows');if(!tbody)return;
 try{
  const query=new URLSearchParams({page:String(state.page),size:'15',sortBy:state.sortBy,direction:state.direction});
  Object.keys(filterLabels).forEach(key=>{if(state[key]!=='')query.set(key,state[key]);});
  const data=await api('/api/people?'+query.toString());
  if (state.page > 0 && !(data.content||[]).length && data.totalElements > 0) {
   state.page = Math.max(0, data.totalPages-1); return renderPeopleRows();
  }
  tbody.innerHTML=(data.content||[]).map(personRow).join('')||`<tr><td colspan="15"><div class="empty">${icon('search',26)}<strong>Ничего не найдено</strong><p>Попробуйте изменить или сбросить фильтры.</p></div></td></tr>`;
  document.getElementById('results').textContent=`Всего записей: ${Number(data.totalElements).toLocaleString('ru-RU')}`;
  document.getElementById('page-label').textContent=`${data.number+1} из ${Math.max(1,data.totalPages)}`;
  document.getElementById('prev').disabled=data.first;
  document.getElementById('next').disabled=data.last;
 }catch(err){tbody.innerHTML=`<tr><td colspan="15"><div class="empty"><strong>Не удалось загрузить людей</strong><p>${E(err.message)}</p></div></td></tr>`;notify(err.message,true);}
}
function fact(name,value){return `<div class="fact"><small>${name}</small><strong>${E(value ?? '—')}</strong></div>`;}
async function detail(){
 try{
  const p=await api(`/api/people/${currentId}`);
  root.innerHTML=pageHead('Карточка человека','',`<a href="/people" class="btn btn-secondary">${icon('back',16)} К списку</a><button id="edit-current" class="btn btn-primary" type="button">${icon('pencil',16)} Изменить</button><button id="delete-current" class="btn btn-danger" type="button">${icon('trash',16)} Удалить</button>`)+
  `<div class="card"><div class="profile-hero"><div class="profile-avatar">${E(p.name.trim().charAt(0).toUpperCase())}</div><div><h2>${E(p.name)}</h2><p>Запись №${p.id} · Добавлена ${E(prettyDate(p.creationDate))}</p></div></div>
  <section class="profile-section"><h3>Основные сведения</h3><div class="detail-grid">${fact('Имя',p.name)}${fact('Гражданство',countryLabel(p.nationality))}${fact('Дата добавления',prettyDate(p.creationDate))}</div></section>
  <section class="profile-section"><h3>Характеристики</h3><div class="detail-grid">${fact('Рост',`${prettyNumber(p.height)} см`)}${fact('Вес',`${prettyNumber(p.weight)} кг`)}${fact('Цвет глаз',colorLabel(p.eyeColor))}${fact('Цвет волос',colorLabel(p.hairColor))}</div></section>
  <section class="profile-section"><h3>Координаты</h3><div class="detail-grid">${fact('X',prettyNumber(p.coordinateX))}${fact('Y',prettyNumber(p.coordinateY))}</div></section>
  <section class="profile-section"><h3>Локация</h3>${p.location?`<div class="location-summary"><div class="location-symbol">${icon('pin',22)}</div><div><a class="location-card-link" href="/locations/${p.location.id}">${E(p.location.name || `Локация №${p.location.id}`)}</a><div class="small-copy">ID ${p.location.id} · X: ${prettyNumber(p.location.x)}, Y: ${prettyNumber(p.location.y)}</div></div></div>`:'<p class="muted">Локация не указана.</p>'}</section></div>`;
  document.getElementById('edit-current').onclick=()=>editPerson(currentId);
  document.getElementById('delete-current').onclick=async()=>{
   if(!await confirmAction({title:'Удалить человека?',description:`Запись «${p.name}» будет удалена без возможности восстановления.`}))return;
   try{await api(`/api/people/${currentId}`,{method:'DELETE'});window.location.assign('/people');}catch(err){notify(err.message,true);}
  };
 }catch(err){root.innerHTML=pageHead('Запись недоступна')+`<div class="card card-body"><p>${E(err.message)}</p><a class="btn btn-secondary" href="/people">Вернуться к списку</a></div>`;}
}
async function newPerson(){await loadLocations();root.classList.add('narrow');root.innerHTML=pageHead('Новый человек','',`<a class="btn btn-secondary" href="/people">${icon('back',16)} К списку</a>`)+`<div class="card">${personForm()}</div>`;wirePersonForm();}
function locationForm(l={}){return `<form id="location-form">
 ${fieldSection('Данные локации',field('Название','lname',`<input id="lname" maxlength="200" placeholder="Например, Центральный офис" value="${E(l.name||'')}">`,'Необязательное поле')+
 field('Координата X *','lx',numberInput('lx',l.x??0,'step="1" required'))+
 field('Координата Y *','ly',numberInput('ly',l.y??0,'required')))}
 <div class="form-actions form-actions-inset"><button class="btn btn-secondary" type="button" id="loc-cancel">Отмена</button><button class="btn btn-primary" type="submit">${icon('check',16)} Сохранить</button></div></form>`;}
async function locationModal(l=null){
 openModal(modalHead(l?'Редактировать локацию':'Новая локация',l?`Запись №${l.id}`:'')+locationForm(l||{}));
 document.getElementById('loc-cancel').onclick=()=>dialog.close();
 document.getElementById('location-form').onsubmit=async event=>{
  event.preventDefault();const button=event.target.querySelector('[type=submit]');button.disabled=true;
  const body={x:Number(document.getElementById('lx').value),y:Number(document.getElementById('ly').value),name:document.getElementById('lname').value.trim()||null,version:l?.version??null};
  try{await api(l?`/api/locations/${l.id}`:'/api/locations',{method:l?'PUT':'POST',body});dialog.close();notify('Локация сохранена');if(mode==='location-detail')await locationDetail();else await renderLocations();}
  catch(err){notify(err.message,true);}finally{button.disabled=false;}
 };
}
async function removeLocation(l){
 const others=locations.filter(other=>other.id!==l.id);
 openModal(modalHead('Удаление локации',`Запись №${l.id}`)+
 `<section class="form-section"><p class="muted">Если к этой локации привязаны люди, необходимо выбрать другую локацию, к которой они будут перенесены.</p>
 ${field('Куда перенести связанные записи','replacement',`<select id="replacement"><option value="">Без переноса (если связей нет)</option>${others.map(other=>`<option value="${other.id}">${E(locationText(other))}</option>`).join('')}</select>`)}</section>
 <div class="form-actions form-actions-inset"><button class="btn btn-secondary" id="loc-cancel">Отмена</button><button class="btn btn-danger" id="loc-remove">${icon('trash',16)} Продолжить</button></div>`);
 document.getElementById('loc-cancel').onclick=()=>dialog.close();
 document.getElementById('loc-remove').onclick=async()=>{
  const target=document.getElementById('replacement').value;
  dialog.close();
  if(!await confirmAction({title:'Удалить локацию?',description:target?'Связанные люди будут перенесены в выбранную локацию.':'Локация будет удалена. Если есть связанные люди, сервер не позволит удалить её без переназначения.'}))return;
  try{await api(`/api/locations/${l.id}${target?'?replacementLocationId='+encodeURIComponent(target):''}`,{method:'DELETE'});notify('Локация удалена');if(mode==='location-detail')window.location.assign('/locations');else await renderLocations();}
  catch(err){notify(err.message,true);}
 };
}
async function locationPage(){
 root.innerHTML=pageHead('Локации','',`<button id="add-location" class="btn btn-primary" type="button">${icon('plus')} Добавить локацию</button>`)+
 `<section class="card"><div class="card-header"><div><h2>Все локации</h2><p class="card-caption" id="locations-count">Загрузка…</p></div></div><div id="location-list" class="table-scroll"></div></section>`;
 document.getElementById('add-location').onclick=()=>locationModal();await renderLocations();
}
async function renderLocations(){
 try {
  await loadLocations();
  const target=document.getElementById('location-list');
  if (!target) return;
  document.getElementById('locations-count').textContent=`Всего: ${locations.length}`;
  target.innerHTML=`<table class="data-table locations-table"><thead><tr><th>Название</th><th>ID</th><th>Координата X</th><th>Координата Y</th><th aria-label="Меню строки"></th></tr></thead><tbody>${locations.map(l=>`<tr class="clickable-row" tabindex="0" data-location-row="${l.id}" aria-label="Открыть локацию ${E(l.name || `№${l.id}`)}"><td><a class="person-name" href="/locations/${l.id}">${E(l.name||`Локация №${l.id}`)}</a></td><td class="numeric">${l.id}</td><td class="numeric">${prettyNumber(l.x)}</td><td class="numeric">${prettyNumber(l.y)}</td><td class="row-menu-cell"><button class="row-more-button" type="button" data-location-more="${l.id}" aria-label="Меню локации ${E(l.name||`№${l.id}`)}" aria-haspopup="menu" title="Действия с локацией">⋯</button></td></tr>`).join('')||`<tr><td colspan="5"><div class="empty">${icon('pin',26)}<strong>Локаций пока нет</strong><p>Добавьте первую локацию, чтобы связывать с ней людей.</p></div></td></tr>`}</tbody></table>`;
  target.onclick=event=>{
   const button=event.target.closest('[data-location-more]');
   if (button) {
    const location = locations.find(l=>l.id===Number(button.dataset.locationMore));
    if (!location) return;
    openRowActions(button, [
     {icon:'eye', label:'Открыть карточку', run:()=>window.location.assign(`/locations/${location.id}`)},
     {icon:'pencil', label:'Изменить', run:()=>locationModal(location)},
     {icon:'trash', label:'Удалить', danger:true, run:()=>removeLocation(location)}
    ]);
    return;
   }
   const row=event.target.closest('[data-location-row]');
   if (row && !isInteractiveTarget(event.target)) window.location.assign(`/locations/${row.dataset.locationRow}`);
  };
  target.onkeydown=event=>{
   if (event.target.matches('[data-location-row]') && (event.key==='Enter'||event.key===' ')){
    event.preventDefault();window.location.assign(`/locations/${event.target.dataset.locationRow}`);
   }
  };
 } catch(err){notify(err.message,true);}
}
async function locationDetail(){
 try {
  const l=await api(`/api/locations/${currentId}`);
  const linked=await api(`/api/locations/${currentId}/people`);
  root.innerHTML=pageHead('Карточка локации','',`<a class="btn btn-secondary" href="/locations">${icon('back',16)} К локациям</a><button id="edit-location" class="btn btn-primary" type="button">${icon('pencil',16)} Изменить</button><button id="delete-location" class="btn btn-danger" type="button">${icon('trash',16)} Удалить</button>`)+
  `<section class="card"><div class="profile-hero"><div class="location-profile-icon">${icon('pin',29)}</div><div><h2>${E(l.name || `Локация №${l.id}`)}</h2><p>ID ${l.id}</p></div></div>
    <div class="profile-section"><h3>Расположение</h3><div class="detail-grid">${fact('Координата X',prettyNumber(l.x))}${fact('Координата Y',prettyNumber(l.y))}</div></div>
    <div class="profile-section"><div class="detail-section-heading"><h3>Связанные люди</h3><span class="linked-count">${linked.length}</span></div>
    ${linked.length?`<div class="linked-people">${linked.map(p=>`<a class="linked-person" href="/people/${p.id}"><span class="linked-person-avatar">${E((p.name||'?').charAt(0).toUpperCase())}</span><span><strong>${E(p.name)}</strong><small>ID ${p.id} · Рост ${prettyNumber(p.height)} см · Вес ${prettyNumber(p.weight)} кг</small></span>${icon('chevron',17)}</a>`).join('')}</div>`:'<p class="muted">Пока к этой локации никто не привязан.</p>'}
    </div></section>`;
  document.getElementById('edit-location').onclick=()=>locationModal(l);
  document.getElementById('delete-location').onclick=async()=>{await loadLocations();await removeLocation(l);};
 }catch(err){root.innerHTML=pageHead('Локация недоступна')+`<div class="card card-body"><p>${E(err.message)}</p><a href="/locations" class="btn btn-secondary">Вернуться к локациям</a></div>`;}
}
function toolCard(iconName,title,description,form,resultId,danger=false){
 return `<article class="card tool-card"><div class="tool-top"><div class="tool-icon${danger?' alert':''}">${icon(iconName,20)}</div><h2>${title}</h2></div><p class="tool-desc">${description}</p>${form}<div id="${resultId}" class="tool-result" role="status" hidden></div></article>`;
}
function reportCount(resultId,count,word='человек'){
 const target=document.getElementById(resultId);target.hidden=false;target.innerHTML=`<div class="result-number">${Number(count).toLocaleString('ru-RU')}</div><p>Найдено записей</p>`;
}
function reportMessage(resultId,title,text=''){
 const target=document.getElementById(resultId);target.hidden=false;target.innerHTML=`<h3>${E(title)}</h3>${text?`<p>${E(text)}</p>`:''}`;
}
async function operations(){
 root.innerHTML=pageHead('Инструменты','Действия с записями и быстрый подсчёт данных.')+`<div class="tools-grid">
 ${toolCard('trash','Удалить по весу','Удалится один человек с точно указанным весом.',`<form id="op-delete">${field('Вес, кг','delete-weight',numberInput('delete-weight','','min="0.00000001" required placeholder="Например, 75"').replace('name="delete-weight"','name="weight"'))}<button class="btn btn-danger" type="submit">Удалить</button></form>`,'r-delete',true)}
 ${toolCard('users','Последняя запись','Показать человека с самым большим номером.',`<div><button class="btn btn-secondary" id="op-max" type="button">${icon('search',16)} Найти человека</button></div>`,'r-max')}
 ${toolCard('chart','Рост ниже значения','Посчитать, сколько людей ниже указанного роста.',`<form id="op-height">${field('Рост, см','height-limit',numberInput('height-limit','','required placeholder="Например, 170"').replace('name="height-limit"','name="height"'))}<button class="btn btn-secondary" type="submit">Посчитать</button></form>`,'r-height')}
 ${toolCard('layers','По цвету волос','Посчитать людей с выбранным цветом волос.',`<form id="op-hair">${field('Цвет волос','hair-color',`<select id="hair-color" name="color">${options(colors,'RED',false,colorNames)}</select>`)}<button class="btn btn-secondary" type="submit">Посчитать</button></form>`,'r-hair')}
 ${toolCard('eye','По цвету глаз','Посчитать людей с выбранным цветом глаз.',`<form id="op-eye">${field('Цвет глаз','eye-color',`<select id="eye-color" name="color">${options(colors,'RED',false,colorNames)}</select>`)}<button class="btn btn-secondary" type="submit">Посчитать</button></form>`,'r-eye')}
 </div>`;
 const runForm=(formId,resultId,path,method='GET')=>{
  document.getElementById(formId).onsubmit=async event=>{
   event.preventDefault();const form=event.target;const button=form.querySelector('button[type=submit]');
   const query=new URLSearchParams(new FormData(form));
   if(method==='POST' && !await confirmAction({title:'Удалить человека?',description:`Будет удалён один человек, вес которого точно равен ${query.get('weight')} кг.`}))return;
   button.disabled=true;
   try{const value=await api(`${path}?${query}`,{method,key:method==='POST'?crypto.randomUUID():undefined});
    if(method==='POST')reportMessage(resultId,value.id!=null?'Человек удалён':'Совпадений нет',value.id!=null?`Удалена запись №${value.id}.`:'Никто не был удалён.');
    else reportCount(resultId,value.count);
    if(method==='POST' && value.id!=null)notify('Запись удалена');
   }catch(err){reportMessage(resultId,'Не удалось выполнить действие',err.message);notify(err.message,true);}
   finally{button.disabled=false;}
  };
 };
 runForm('op-delete','r-delete','/api/operations/delete-by-weight','POST');
 runForm('op-height','r-height','/api/operations/count-height');
 runForm('op-hair','r-hair','/api/operations/count-hair');
 runForm('op-eye','r-eye','/api/operations/count-eyes');
 document.getElementById('op-max').onclick=async()=>{
  try{const p=await api('/api/operations/max-id');const target=document.getElementById('r-max');target.hidden=false;
   target.innerHTML=p?`<h3>${E(p.name)}</h3><p>Запись №${p.id}</p><a href="/people/${p.id}" class="btn btn-small btn-secondary" style="margin-top:10px">Открыть карточку ${icon('chevron',13)}</a>`:'<h3>Записей пока нет</h3><p>Добавьте человека в каталоге.</p>';
  }catch(err){notify(err.message,true);}
 };
}
const actions={CREATE:'Создание',UPDATE:'Изменение',DELETE:'Удаление',DELETE_BY_WEIGHT:'Удаление по весу',DELETE_REASSIGN:'Удаление и перенос связей',REGISTER:'Регистрация'};
const entities={PERSON:'Человек',LOCATION:'Локация',USER:'Пользователь'};
async function audit(){
 root.innerHTML=pageHead('История изменений','Действия пользователей с записями.')+`<section class="card"><div class="table-scroll" id="audit-list"><div class="loading-state">Загрузка истории…</div></div></section>`;
 await loadAudit();
}
async function loadAudit(){
 const target=document.getElementById('audit-list');if(!target)return;
 try{
  const events=await api('/api/audit');
  target.innerHTML=`<table class="data-table" style="min-width:880px"><thead><tr><th>Когда</th><th>Кто</th><th>Запись</th><th>Действие</th><th>Подробности</th></tr></thead><tbody>${events.map(e=>`<tr><td>${E(prettyDate(e.at))}</td><td class="person-name">${E(e.actor)}</td><td>${E(entities[e.entity]||e.entity)} ${e.entityId!=null?'№'+E(e.entityId):''}</td><td><span class="chip">${E(actions[e.action]||e.action)}</span></td><td class="audit-details"><details><summary>Посмотреть</summary><pre>${E(e.details)}</pre></details></td></tr>`).join('')||`<tr><td colspan="5"><div class="empty">${icon('clock',26)}<strong>История пока пуста</strong><p>Здесь появятся действия пользователей.</p></div></td></tr>`}</tbody></table>`;
 }catch(err){notify(err.message,true);}
}
async function refresh(){
 if(dialog.open || confirmationDialog.open)return;
 switch(mode){case 'list':await renderPeopleRows();break;case 'detail':await detail();break;case 'locations':await renderLocations();break;case 'location-detail':await locationDetail();break;case 'audit':await loadAudit();break;default:break;}
}
dialog.addEventListener('close',()=>{if(mode!=='new' && mode!=='operations')refresh();});
let refreshTimer;
function connectEvents(){
 const url=(location.protocol==='https:'?'wss://':'ws://')+location.host+'/ws/events';
 try{const socket=new WebSocket(url);
  socket.onmessage=()=>{clearTimeout(refreshTimer);refreshTimer=setTimeout(()=>{refresh();notify('Данные обновлены');},240);};
  socket.onclose=()=>setTimeout(connectEvents,3500);
 }catch(err){console.warn('Не удалось подключить синхронизацию',err);setTimeout(connectEvents,5000);}
}
(async()=>{
 try{
  switch(mode){case 'list':await listPeople();break;case 'new':await newPerson();break;case 'detail':await detail();break;case 'locations':await locationPage();break;case 'location-detail':await locationDetail();break;case 'operations':await operations();break;case 'audit':await audit();break;default:root.innerHTML='<div class="card card-body">Страница не найдена.</div>';}
  root.setAttribute('aria-busy','false');connectEvents();
 }catch(err){root.innerHTML=`<div class="card card-body"><h2>Не удалось открыть страницу</h2><p>${E(err.message)}</p><a href="/people" class="btn btn-secondary">К списку людей</a></div>`;notify(err.message,true);root.setAttribute('aria-busy','false');}
})();
