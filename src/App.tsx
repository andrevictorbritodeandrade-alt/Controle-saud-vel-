import { useState } from 'react';
import { format, parseISO, startOfMonth, getDaysInMonth, addDays } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { Cloud, Check, Droplets, Moon, Sun, Home, History, Calendar, RefreshCcw } from 'lucide-react';
import { AreaChart, Area, XAxis, Tooltip, ResponsiveContainer } from 'recharts';

type Tab = 'home' | 'history';

function App() {
  const [activeTab, setActiveTab] = useState<Tab>('home');
  const [selectedDate, setSelectedDate] = useState(() => format(new Date(), 'yyyy-MM-dd'));

  return (
    <div className="min-h-screen bg-slate-900 text-slate-50 flex flex-col md:max-w-md md:mx-auto md:shadow-xl md:shadow-emerald-900/20">
      <header className="bg-slate-800 p-4 sticky top-0 z-10 border-b border-slate-700 flex justify-between items-center shadow-md">
        <div>
          <h1 className="text-xl font-bold text-emerald-400 flex items-center gap-2">
            <Sun className="w-6 h-6" /> Rotina Cortisol
          </h1>
          <p className="text-xs text-slate-400">
            {format(parseISO(selectedDate), "EEEE, d 'de' MMMM", { locale: ptBR })}
          </p>
        </div>
        <div className="flex gap-2">
           <div className="bg-sky-500/10 border border-sky-500/30 rounded-full px-3 py-1 flex items-center gap-1">
             <Cloud className="w-3 h-3 text-sky-400" />
             <span className="text-[10px] font-bold text-sky-400">FIREBASE</span>
           </div>
        </div>
      </header>

      <main className="flex-1 overflow-y-auto p-4 pb-24">
        {activeTab === 'home' ? (
          <HomeTab selectedDate={selectedDate} />
        ) : (
          <HistoryTab onSelectDate={(date) => { setSelectedDate(date); setActiveTab('home'); }} />
        )}
      </main>

      <nav className="fixed bottom-0 w-full md:max-w-md bg-slate-800 border-t border-slate-700 flex justify-around p-3 pb-safe shadow-[0_-4px_10px_rgba(0,0,0,0.3)]">
        <button 
          onClick={() => setActiveTab('home')}
          className={`flex flex-col items-center gap-1 ${activeTab === 'home' ? 'text-emerald-400' : 'text-slate-400'}`}
        >
          <Home className="w-6 h-6" />
          <span className="text-xs font-medium">Hoje</span>
        </button>
        <button 
          onClick={() => setActiveTab('history')}
          className={`flex flex-col items-center gap-1 ${activeTab === 'history' ? 'text-emerald-400' : 'text-slate-400'}`}
        >
          <History className="w-6 h-6" />
          <span className="text-xs font-medium">Histórico</span>
        </button>
      </nav>
    </div>
  );
}

// -----------------------------------------------------------------------------
// LOCAL STORAGE HOOKS
// -----------------------------------------------------------------------------
function useLocalStorage<T>(key: string, initialValue: T) {
  const [storedValue, setStoredValue] = useState<T>(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch (error) {
      return initialValue;
    }
  });

  const setValue = (value: T | ((val: T) => T)) => {
    try {
      const valueToStore = value instanceof Function ? value(storedValue) : value;
      setStoredValue(valueToStore);
      window.localStorage.setItem(key, JSON.stringify(valueToStore));
    } catch (error) {
      console.log(error);
    }
  };

  return [storedValue, setValue] as const;
}

// -----------------------------------------------------------------------------
// HOME TAB
// -----------------------------------------------------------------------------
const defaultTasks = [
  { id: '1', title: 'Acordar e ver luz solar', time: '06:30' },
  { id: '2', title: 'Café da manhã rico em proteína', time: '07:30' },
  { id: '3', title: 'Atividade física', time: '17:00' },
  { id: '4', title: 'Diminuir luzes azuis', time: '20:30' },
];

function HomeTab({ selectedDate }: { selectedDate: string }) {
  const [tasks, setTasks] = useLocalStorage(`tasks_${selectedDate}`, defaultTasks.map(t => ({...t, completed: false})));
  const [water, setWater] = useLocalStorage(`water_${selectedDate}`, 0);
  const [sleep, setSleep] = useLocalStorage(`sleep_${selectedDate}`, { bedtime: '22:00', wakeTime: '06:00' });

  const toggleTask = (id: string) => {
    setTasks(tasks.map(t => t.id === id ? { ...t, completed: !t.completed } : t));
    
    // Update daily completion record for history
    const history = JSON.parse(localStorage.getItem('history') || '{}');
    const isAnyCompleted = tasks.some(t => t.id === id ? !t.completed : t.completed);
    history[selectedDate] = { executed: isAnyCompleted, tasksCount: tasks.length };
    localStorage.setItem('history', JSON.stringify(history));
  };

  return (
    <div className="space-y-6">
      <section className="bg-slate-800 p-4 rounded-2xl border border-slate-700 shadow-lg">
        <h2 className="text-lg font-bold mb-4 flex items-center gap-2 text-white">
          <Check className="text-emerald-400" /> Tarefas para Redução de Cortisol
        </h2>
        <div className="space-y-3">
          {tasks.map(task => (
            <div 
              key={task.id} 
              onClick={() => toggleTask(task.id)}
              className={`flex items-center justify-between p-3 rounded-xl border cursor-pointer transition-colors ${
                task.completed ? 'bg-emerald-900/20 border-emerald-500/50' : 'bg-slate-700/50 border-slate-600'
              }`}
            >
              <div>
                <p className={`font-medium ${task.completed ? 'text-emerald-300 line-through' : 'text-slate-200'}`}>
                  {task.title}
                </p>
                <p className="text-xs text-slate-400">{task.time}</p>
              </div>
              <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center ${
                task.completed ? 'bg-emerald-500 border-emerald-500' : 'border-slate-500'
              }`}>
                {task.completed && <Check className="w-4 h-4 text-white" />}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-slate-800 p-4 rounded-2xl border border-slate-700 shadow-lg">
        <h2 className="text-lg font-bold mb-4 flex items-center gap-2 text-white">
          <Droplets className="text-blue-400" /> Hidratação
        </h2>
        <div className="flex items-center justify-between">
          <div className="text-2xl font-bold text-blue-300">{water} ml</div>
          <div className="flex gap-2">
            <button 
              onClick={() => setWater(w => Math.max(0, w - 250))}
              className="bg-slate-700 p-2 rounded-lg text-slate-300 hover:bg-slate-600"
            >-250</button>
            <button 
              onClick={() => setWater(w => w + 250)}
              className="bg-blue-600 p-2 rounded-lg text-white hover:bg-blue-500"
            >+250ml</button>
          </div>
        </div>
      </section>

      <section className="bg-slate-800 p-4 rounded-2xl border border-slate-700 shadow-lg">
        <h2 className="text-lg font-bold mb-4 flex items-center gap-2 text-white">
          <Moon className="text-indigo-400" /> Registro de Sono
        </h2>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-slate-300">🛏️ Deitou às:</span>
            <input 
              type="time" 
              value={sleep.bedtime} 
              onChange={e => setSleep({...sleep, bedtime: e.target.value})}
              className="bg-slate-900 border border-slate-700 rounded p-1 text-white"
            />
          </div>
          <div className="flex gap-2">
             <button onClick={() => setSleep({...sleep, bedtime: '22:00'})} className="flex-1 text-xs border border-slate-600 p-2 rounded-lg hover:bg-indigo-900/30 text-indigo-300">22:00</button>
             <button onClick={() => setSleep({...sleep, bedtime: '22:30'})} className="flex-1 text-xs border border-slate-600 p-2 rounded-lg hover:bg-indigo-900/30 text-indigo-300">22:30</button>
             <button onClick={() => setSleep({...sleep, bedtime: '23:00'})} className="flex-1 text-xs border border-slate-600 p-2 rounded-lg hover:bg-indigo-900/30 text-indigo-300">23:00</button>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-slate-300">☀️ Acordou às:</span>
            <input 
              type="time" 
              value={sleep.wakeTime} 
              onChange={e => setSleep({...sleep, wakeTime: e.target.value})}
              className="bg-slate-900 border border-slate-700 rounded p-1 text-white"
            />
          </div>
        </div>
      </section>
    </div>
  );
}

// -----------------------------------------------------------------------------
// HISTORY TAB
// -----------------------------------------------------------------------------
function HistoryTab({ onSelectDate }: { onSelectDate: (date: string) => void }) {
  console.log(onSelectDate);
  const history = JSON.parse(localStorage.getItem('history') || '{}');
  const [currentMonth] = useState(startOfMonth(new Date()));
  
  const daysInMonth = getDaysInMonth(currentMonth);
  let daysExecuted = 0;
  
  const dailyLogs = [];
  for (let i = 1; i <= daysInMonth; i++) {
    const d = format(addDays(currentMonth, i - 1), 'yyyy-MM-dd');
    const isExecuted = history[d]?.executed || false;
    if (isExecuted) daysExecuted++;
    
    dailyLogs.push({
      date: d,
      displayDate: format(addDays(currentMonth, i - 1), 'dd/MM/yyyy'),
      isExecuted,
      executedCountSoFar: daysExecuted,
      totalDaysInMonth: daysInMonth
    });
  }
  
  const progressPercent = (daysExecuted / daysInMonth) * 100;

  // Chart data for weekly view
  const chartData = [
    { name: 'Seg', percent: 80 },
    { name: 'Ter', percent: 100 },
    { name: 'Qua', percent: 50 },
    { name: 'Qui', percent: 75 },
    { name: 'Sex', percent: 100 },
    { name: 'Sáb', percent: 25 },
    { name: 'Dom', percent: Math.round(progressPercent) || 0 },
  ];

  return (
    <div className="space-y-6">
      
      {/* Monthly Evolution Card */}
      <div className="bg-slate-800 rounded-2xl border-2 border-emerald-500/50 p-4 shadow-lg overflow-hidden relative">
        <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/10 blur-3xl rounded-full" />
        
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-base font-bold text-white flex items-center gap-2">
            <Calendar className="w-5 h-5 text-emerald-400" />
            Evolução Mensal
          </h2>
          <div className="bg-sky-500/10 border border-sky-500/30 rounded-full px-2 py-1 flex items-center gap-1">
             <Cloud className="w-3 h-3 text-sky-400" />
             <span className="text-[9px] font-bold text-sky-400">FIREBASE NUVEM</span>
          </div>
        </div>

        <div className="flex justify-between items-end mb-2">
          <div>
            <p className="text-emerald-300 font-bold text-lg">Mês: {format(currentMonth, "MMMM 'de' yyyy", { locale: ptBR })}</p>
            <p className="text-xs text-slate-400">Meta 30 dias para redução de cortisol</p>
          </div>
          <button className="p-2 bg-slate-700/50 rounded-full hover:bg-slate-700">
             <RefreshCcw className="w-4 h-4 text-slate-400" />
          </button>
        </div>

        <div className="my-4">
          <div className="flex justify-between text-sm font-medium mb-1">
             <span>Dias executados: {daysExecuted} de {daysInMonth}</span>
             <span className="text-emerald-400">{Math.round(progressPercent)}% concluído</span>
          </div>
          <div className="w-full bg-slate-900 h-3 rounded-full overflow-hidden border border-slate-700">
            <div 
              className="bg-gradient-to-r from-emerald-500 to-sky-400 h-full rounded-full"
              style={{ width: `${Math.max(2, progressPercent)}%` }}
            />
          </div>
        </div>

        <div>
          <p className="text-xs font-bold text-slate-400 mb-2">📋 Registro diário de execução:</p>
          <div className="space-y-2 max-h-60 overflow-y-auto pr-2 custom-scrollbar">
            {dailyLogs.reverse().map((log, idx) => (
              <div 
                key={idx}
                className={`p-3 rounded-xl border flex justify-between items-center ${
                  log.isExecuted 
                    ? 'bg-emerald-900/20 border-emerald-500/50' 
                    : 'bg-slate-900 border-slate-700'
                }`}
              >
                <div>
                  <p className="font-bold text-sm text-white">Dia {log.displayDate}</p>
                  <p className={`text-xs ${log.isExecuted ? 'text-emerald-300' : 'text-slate-500'}`}>
                    {log.isExecuted 
                      ? `dias executados ${log.executedCountSoFar} de ${log.totalDaysInMonth} feito`
                      : `dias executados ${log.executedCountSoFar} de ${log.totalDaysInMonth} (pendente)`}
                  </p>
                </div>
                <div className={`px-2 py-1 rounded-md text-[10px] font-bold ${
                  log.isExecuted ? 'bg-emerald-500/20 text-emerald-300' : 'bg-slate-700 text-slate-400'
                }`}>
                  {log.isExecuted ? '✅ Feito' : '⏳ Pendente'}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Weekly Progress Chart */}
      <div className="bg-slate-800 rounded-2xl border border-slate-700 p-4 shadow-lg">
         <h2 className="text-base font-bold text-white flex items-center gap-2 mb-4">
            📈 Cumprimento Semanal
         </h2>
         <div className="h-48 w-full bg-slate-900/50 rounded-xl p-2 pt-4">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorPercent" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.5}/>
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis dataKey="name" stroke="#64748b" fontSize={12} tickLine={false} axisLine={false} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#1e293b', border: 'none', borderRadius: '8px', color: '#fff' }}
                  itemStyle={{ color: '#10b981' }}
                />
                <Area type="monotone" dataKey="percent" stroke="#38bdf8" strokeWidth={3} fillOpacity={1} fill="url(#colorPercent)" />
              </AreaChart>
            </ResponsiveContainer>
         </div>
      </div>

    </div>
  );
}

export default App;
