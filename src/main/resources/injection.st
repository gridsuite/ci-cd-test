for (equipment in network.<collectionName>) {
  <if(nominalV || nominalVMin || countries)>if (<if(nominalV)>(equipment.terminal.voltageLevel.nominalV <nominalVOperator> <nominalV>)<endif>
    <if(nominalVMin)>(equipment.terminal.voltageLevel.nominalV >= <nominalVMin> && equipment.terminal.voltageLevel.nominalV \<= <nominalVMax>)<endif>
    <if((nominalV || nominalVMin) && countries)>&& <endif><if(countries)>injectionMatch(equipment.terminal, <countries>)<endif>
   ) {
   <endif>
        contingency(equipment.id) { equipments equipment.id }
   <if(nominalV || nominalVMin || countries)>
  }
  <endif>
}
