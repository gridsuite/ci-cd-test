for (equipment in network.<collectionName>) {
  <if(nominalV || nominalVMin || nominalV1 || nominalV2 || nominalVMin1 || nominalVMin2 || countries)>if (
     <if(nominalV)>(equipment.terminal1.voltageLevel.nominalV <nominalVOperator> <nominalV>
     || equipment.terminal2.voltageLevel.nominalV <nominalVOperator> <nominalV>)<endif>
     <if(nominalVMin)>((equipment.terminal1.voltageLevel.nominalV >= <nominalVMin> && equipment.terminal1.voltageLevel.nominalV \<= <nominalVMax>)
     || (equipment.terminal2.voltageLevel.nominalV >= <nominalVMin> && equipment.terminal2.voltageLevel.nominalV \<= <nominalVMax>))<endif>
     <if(nominalV1 && nominalV2)>((equipment.terminal1.voltageLevel.nominalV <nominalVOperator1> <nominalV1> && equipment.terminal2.voltageLevel.nominalV <nominalVOperator2> <nominalV2>)
     || (equipment.terminal1.voltageLevel.nominalV <nominalVOperator2> <nominalV2> && equipment.terminal2.voltageLevel.nominalV <nominalVOperator1> <nominalV1>))<endif>
     <if(nominalV1 && nominalVMin2)>((equipment.terminal1.voltageLevel.nominalV <nominalVOperator1> <nominalV1> && equipment.terminal2.voltageLevel.nominalV >= <nominalVMin2> && equipment.terminal2.voltageLevel.nominalV \<= <nominalVMax2>)
     || (equipment.terminal1.voltageLevel.nominalV >= <nominalVMin2> && equipment.terminal1.voltageLevel.nominalV \<= <nominalVMax2> && equipment.terminal2.voltageLevel.nominalV <nominalVOperator1> <nominalV1>))<endif>
     <if(nominalVMin1 && nominalV2)>((equipment.terminal1.voltageLevel.nominalV >= <nominalVMin1> && equipment.terminal1.voltageLevel.nominalV \<= <nominalVMax1> && equipment.terminal2.voltageLevel.nominalV <nominalVOperator2> <nominalV2>)
     || (equipment.terminal1.voltageLevel.nominalV <nominalVOperator2> <nominalV2> && equipment.terminal2.voltageLevel.nominalV >= <nominalVMin1> && equipment.terminal2.voltageLevel.nominalV \<= <nominalVMax1>))<endif>
     <if(nominalVMin1 && nominalVMin2)>((equipment.terminal1.voltageLevel.nominalV >= <nominalVMin1> && equipment.terminal1.voltageLevel.nominalV \<= <nominalVMax1> && equipment.terminal2.voltageLevel.nominalV >= <nominalVMin2> && equipment.terminal2.voltageLevel.nominalV \<= <nominalVMax2>)
     || (equipment.terminal1.voltageLevel.nominalV >= <nominalVMin2> && equipment.terminal1.voltageLevel.nominalV \<= <nominalVMax2> && equipment.terminal2.voltageLevel.nominalV >= <nominalVMin1> && equipment.terminal2.voltageLevel.nominalV \<= <nominalVMax1>))<endif>
     <if((nominalV || nominalVMin || nominalV1 || nominalV2 || nominalVMin1 || nominalVMin2) && countries)>
     && <endif><if(countries)>transfoMatch(equipment, <countries>)<endif>
   ) {
     <endif>
           contingency(equipment.id) { equipments equipment.id }
   <if(nominalV || nominalVMin || nominalV1 || nominalV2 || nominalVMin1 || nominalVMin2 || countries)>
  }
  <endif>
}


