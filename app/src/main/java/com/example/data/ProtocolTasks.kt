package com.example.data

data class ProtocolTask(
    val id: String,
    val text: String,
    val icon: String,
    val targetTime: String? = null
)

object ProtocolTasks {
    val rotinas = mapOf(
        "Segunda" to listOf(
            ProtocolTask("seg1", "Acordar às 04:00", "🌅", "04:00"),
            ProtocolTask("seg2", "Beber 500ml de água", "💧"),
            ProtocolTask("seg3", "Aguardar 90 min: Café preto só às 05:30", "☕", "05:30"),
            ProtocolTask("seg4", "Ativar Tela Preto e Branco no celular (19h)", "📱", "19:00"),
            ProtocolTask("seg5", "Usar óculos bloqueador de luz azul (noite)", "👓", "19:00"),
            ProtocolTask("seg6", "Dormir entre 21h00 - 21h30", "😴", "21:00")
        ),
        "Terça" to listOf(
            ProtocolTask("ter1", "Acordar às 07:30 (Sem negociação)", "🌅", "07:30"),
            ProtocolTask("ter2", "Luz solar direta nos olhos (30 min)", "☀️"),
            ProtocolTask("ter3", "Aguardar 90 min: Café preto só às 09:00", "☕", "09:00"),
            ProtocolTask("ter4", "Descanso ativo: Ficar fora da cama", "🏃"),
            ProtocolTask("ter5", "Ativar Tela Preto e Branco no celular (19h)", "📱", "19:00"),
            ProtocolTask("ter6", "Dormir às 23:00 (Não virar a madrugada)", "😴", "23:00")
        ),
        "Quarta" to listOf(
            ProtocolTask("qua1", "Acordar às 07:30", "🌅", "07:30"),
            ProtocolTask("qua2", "Luz solar direta nos olhos", "☀️"),
            ProtocolTask("qua3", "Treino moderado (30-40 min)", "💪"),
            ProtocolTask("qua4", "Aguardar 90 min: Café preto só às 09:00", "☕", "09:00"),
            ProtocolTask("qua5", "Ativar Tela Preto e Branco no celular (19h)", "📱", "19:00"),
            ProtocolTask("qua6", "Dormir às 23:00", "😴", "23:00")
        ),
        "Quinta" to listOf(
            ProtocolTask("qui1", "Acordar às 07:30", "🌅", "07:30"),
            ProtocolTask("qui2", "Luz solar direta nos olhos", "☀️"),
            ProtocolTask("qui3", "Aguardar 90 min: Café preto só às 09:00", "☕", "09:00"),
            ProtocolTask("qui4", "Ativar Tela Preto e Branco no celular (19h)", "📱", "19:00"),
            ProtocolTask("qui5", "Dormir entre 21h00 - 21h30 (Véspera de trabalho)", "😴", "21:00")
        ),
        "Sexta" to listOf(
            ProtocolTask("sex1", "Acordar às 04:00", "🌅", "04:00"),
            ProtocolTask("sex2", "Beber 500ml de água", "💧"),
            ProtocolTask("sex3", "Aguardar 90 min: Café preto só às 05:30", "☕", "05:30"),
            ProtocolTask("sex4", "Ativar Tela Preto e Branco no celular (19h)", "📱", "19:00"),
            ProtocolTask("sex5", "Usar óculos bloqueador de luz azul (noite)", "👓", "19:00"),
            ProtocolTask("sex6", "Dormir cedo, respeitar o cansaço", "😴")
        ),
        "Sábado" to listOf(
            ProtocolTask("sab1", "Acordar às 07:30", "🌅", "07:30"),
            ProtocolTask("sab2", "Luz solar direta nos olhos", "☀️"),
            ProtocolTask("sab3", "Treino moderado (30-40 min)", "💪"),
            ProtocolTask("sab4", "Aguardar 90 min: Café preto só às 09:00", "☕", "09:00"),
            ProtocolTask("sab5", "Ativar Tela Preto e Branco no celular (19h)", "📱", "19:00"),
            ProtocolTask("sab6", "Dormir às 23:00", "😴", "23:00")
        ),
        "Domingo" to listOf(
            ProtocolTask("dom1", "Acordar às 07:30", "🌅", "07:30"),
            ProtocolTask("dom2", "Luz solar direta nos olhos", "☀️"),
            ProtocolTask("dom3", "Treino moderado (30-40 min)", "💪"),
            ProtocolTask("dom4", "Aguardar 90 min: Café preto só às 09:00", "☕", "09:00"),
            ProtocolTask("dom5", "Ativar Tela Preto e Branco no celular (19h)", "📱", "19:00"),
            ProtocolTask("dom6", "Dormir entre 21h00 - 21h30 (Véspera de trabalho)", "😴", "21:00")
        )
    )

    fun findTask(taskId: String): ProtocolTask? {
        for (tasks in rotinas.values) {
            val found = tasks.find { it.id == taskId }
            if (found != null) return found
        }
        return null
    }
}
